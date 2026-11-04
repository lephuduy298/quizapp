package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import com.example.learning.ReviewRating
import com.example.learning.ReviewScheduler

class ReviewRepository(private val db: AppDatabase, private val clock: () -> Long = { System.currentTimeMillis() }) {
    private val dao = db.reviewDao()
    val questions = db.learningDao().observeQuestions()
    val quizzes = db.quizDao().getAllQuizzesFlow()
    val skills = db.learningDao().observeSkills()
    fun progress(user: String) = dao.observeProgress(LearningRepository.userKey(user))

    suspend fun summary(user: String, now: Long = clock()) = db.withTransaction {
        com.example.learning.ReviewSummary.calculate(
            db.learningDao().questions(), dao.allProgress(LearningRepository.userKey(user)), now)
    }

    suspend fun rate(user: String, questionId: Long, version: Int, rating: ReviewRating, eventKey: String) =
        db.withTransaction { record(user, questionId, version, rating, eventKey, null) }

    suspend fun recordAnswer(user: String, questionId: Long, version: Int, option: Int, eventKey: String) =
        db.withTransaction {
            val question = dao.question(questionId) ?: return@withTransaction false
            if (question.contentVersion != version || option !in question.options.indices ||
                question.correctOptionIndex !in question.options.indices) return@withTransaction false
            record(user, questionId, version,
                if (option == question.correctOptionIndex) ReviewRating.GOOD else ReviewRating.AGAIN,
                eventKey, option)
        }

    private suspend fun record(user: String, questionId: Long, version: Int, rating: ReviewRating,
        eventKey: String, option: Int?): Boolean {
        require(eventKey.isNotBlank())
        val key = LearningRepository.userKey(user)
        require(key.isNotBlank())
        val question = dao.question(questionId) ?: return false
        if (question.contentVersion != version || question.options.size < 2 ||
            question.correctOptionIndex !in question.options.indices) return false
        val now = clock()
        if (dao.insertEvent(ReviewEventEntity(key, eventKey, questionId, version, now)) == -1L) {
            val saved = dao.event(key, eventKey)
            return saved?.questionId == questionId && saved.contentVersion == version
        }
        val original = dao.progress(key, questionId)?.let { ReviewScheduler.current(it, version, now) }
            ?: ReviewProgressEntity(key, questionId, version)
        var updated = ReviewScheduler.rate(original, rating, now)
        // Self-reported flashcard memory is not counted as a wrong multiple-choice answer.
        if (option != null && option != question.correctOptionIndex) updated = updated.copy(
            mistakeCount = original.mistakeCount + 1, lastWrongAt = now, lastSelectedOption = option)
        dao.save(updated)
        return true
    }

    suspend fun annotate(user: String, questionId: Long, note: String, favorite: Boolean) = db.withTransaction {
        require(note.length <= 5000) { "Ghi chú tối đa 5000 ký tự." }
        val key = LearningRepository.userKey(user)
        require(key.isNotBlank())
        val question = dao.question(questionId) ?: error("Câu hỏi đã bị xóa.")
        val progress = dao.progress(key, questionId)?.let { ReviewScheduler.current(it, question.contentVersion, clock()) }
            ?: ReviewProgressEntity(key, questionId, question.contentVersion)
        dao.save(progress.copy(note = note.trim(), isFavorite = favorite))
    }

    // Save the exam and its review evidence atomically. Only selected answers are graded.
    suspend fun saveQuizSession(user: String, session: QuizSessionEntity, questions: List<QuestionEntity>,
        eventPrefix: String): Long = db.withTransaction {
        val id = db.quizSessionDao().insertSession(session)
        questions.forEach { q -> session.answersMap[q.id]?.let { option ->
            recordAnswer(user, q.id, q.contentVersion, option, "$eventPrefix:${q.id}")
        } }
        id
    }
}
