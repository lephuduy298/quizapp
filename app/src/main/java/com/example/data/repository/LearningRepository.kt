package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.learning.*
import java.util.Locale

class LearningRepository(private val db: AppDatabase) {
    private val dao = db.learningDao()
    val skills = dao.observeSkills()
    val questions = dao.observeQuestions()
    val quizzes = db.quizDao().getAllQuizzesFlow()
    val folders = db.folderDao().getAllFoldersFlow()
    fun attempts(user: String) = dao.observeAttempts(user)
    fun latestSession(user: String) = dao.observeLatestSession(user)

    companion object { fun userKey(name: String) = name.trim().lowercase(Locale.ROOT) }

    suspend fun classify(ids: List<Long>, subject: String, name: String, difficulty: Int) {
        require(ids.isNotEmpty() && subject.isNotBlank() && name.isNotBlank())
        require(difficulty in 1..3)
        db.withTransaction {
            val subjectName = subject.trim()
            val skillName = name.trim()
            dao.insertSkill(SkillEntity(subject = subjectName, name = skillName))
            val skill = requireNotNull(dao.findSkill(subjectName, skillName))
            dao.classify(ids, skill.id, difficulty)
        }
    }

    private suspend fun bank(session: LearningSessionEntity): List<QuestionEntity> {
        return dao.scopedQuestions(session.quizId, session.folderId, session.skillId)
    }

    suspend fun start(user: String, count: Int, quizId: Long?, folderId: Long?, skillId: Long?) = db.withTransaction {
        require(count in listOf(5, 10, 15))
        val unfinished = dao.unfinished(user)
        check(unfinished == null) { "Hãy tiếp tục hoặc kết thúc phiên đang làm trước." }
        val skills = dao.skills()
        val history = dao.attempts(user)
        val initial = LearningSessionEntity(userKey = user, quizId = quizId, folderId = folderId,
            skillId = skillId, targetCount = count)
        val questions = bank(initial)
        val choice = AdaptiveQuestionSelector.choose(questions, skills, history, emptyList())
            ?: error("Chưa có câu hỏi hợp lệ được gắn kỹ năng trong phạm vi này.")
        val baseline = MasteryCalculator.calculate(skills, questions, history).associate { it.skill.id to it.score }
        dao.insertSession(initial.copy(currentQuestionId = choice.question.id, currentVersion = choice.question.contentVersion,
            reason = choice.reason, baseline = baseline))
    }

    suspend fun answer(user: String, sessionId: Long, questionId: Long?, version: Int, option: Int?, confidence: Int?, elapsed: Long) = db.withTransaction {
        val session = dao.session(sessionId, user) ?: return@withTransaction
        if (session.completedAt != null || session.answered || session.currentQuestionId != questionId || session.currentVersion != version) return@withTransaction
        val question = bank(session).find { it.id == session.currentQuestionId }
        check(question != null && question.contentVersion == session.currentVersion) {
            "Câu hỏi đã thay đổi hoặc bị xóa. Bấm bỏ qua để chọn câu khác."
        }
        require(option == null || option in question.options.indices)
        require(option == null || (confidence != null && confidence in 1..3))
        val inserted = dao.insertAttempt(QuestionAttemptEntity(sessionId = session.id, userKey = user, questionId = question.id,
            contentVersion = question.contentVersion, skillId = requireNotNull(question.skillId), difficulty = question.difficulty,
            correct = option == question.correctOptionIndex, skipped = option == null,
            selectedOption = option, confidence = if (option == null) null else confidence, activeMillis = elapsed.coerceAtLeast(0)))
        if (inserted != -1L && option != null) ReviewRepository(db).recordAnswer(user, question.id,
            question.contentVersion, option, "adaptive:${session.id}:${question.id}")
        dao.updateSession(session.copy(answered = true, selectedOption = option))
    }

    suspend fun next(user: String, sessionId: Long, questionId: Long?, version: Int, skip: Boolean = false) = db.withTransaction {
        var session = dao.session(sessionId, user) ?: return@withTransaction
        if (session.completedAt != null || session.currentQuestionId != questionId || session.currentVersion != version) return@withTransaction
        if (!session.answered) {
            if (!skip) return@withTransaction
            val question = dao.questions().find { it.id == session.currentQuestionId }
            if (question != null) dao.insertAttempt(QuestionAttemptEntity(sessionId = session.id, userKey = user,
                questionId = question.id, contentVersion = question.contentVersion, skillId = question.skillId ?: 0,
                difficulty = question.difficulty, correct = false, skipped = true))
            session = session.copy(answered = true)
        }
        val history = dao.attempts(user)
        val attempts = history.filter { it.sessionId == session.id }
        val choice = if (attempts.count { !it.skipped } >= session.targetCount) null else
            AdaptiveQuestionSelector.choose(bank(session), dao.skills(), history, attempts)
        if (choice == null) dao.updateSession(session.copy(completedAt = System.currentTimeMillis(), currentQuestionId = null))
        else dao.updateSession(session.copy(currentQuestionId = choice.question.id, currentVersion = choice.question.contentVersion,
            reason = choice.reason, answered = false, selectedOption = null, accumulatedMillis = 0))
    }

    suspend fun saveElapsed(user: String, id: Long, questionId: Long, version: Int, millis: Long) = db.withTransaction {
        val session = dao.session(id, user) ?: return@withTransaction
        if (session.completedAt == null && !session.answered && session.currentQuestionId == questionId && session.currentVersion == version)
            dao.updateSession(session.copy(accumulatedMillis = maxOf(session.accumulatedMillis, millis)))
    }

    suspend fun finish(user: String, id: Long) = db.withTransaction {
        val session = dao.session(id, user) ?: return@withTransaction
        if (session.completedAt == null) dao.updateSession(session.copy(completedAt = System.currentTimeMillis(), currentQuestionId = null))
    }

    suspend fun installDemo() = db.withTransaction { LearningDemo.install(db) }
}
