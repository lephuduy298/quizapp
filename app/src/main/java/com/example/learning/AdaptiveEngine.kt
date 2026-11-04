package com.example.learning

import com.example.data.local.*
import kotlin.math.roundToInt

enum class MasteryStatus(val label: String) {
    NEW("Chưa học"), INSUFFICIENT("Chưa đủ dữ liệu"), LEARNING("Đang luyện"), MASTERED("Đã vững")
}

data class SkillMastery(
    val skill: SkillEntity,
    val score: Int,
    val distinctQuestions: Int,
    val availableQuestions: Int,
    val recentCount: Int,
    val mistakes: Int,
    val status: MasteryStatus
) {
    val needsFocus: Boolean get() = recentCount >= 3 && score < 60
}

object MasteryCalculator {
    fun validAttempts(questions: List<QuestionEntity>, attempts: List<QuestionAttemptEntity>): List<QuestionAttemptEntity> {
        val bank = questions.associateBy { it.id }
        return attempts.filter { a -> !a.skipped && bank[a.questionId]?.let {
            it.contentVersion == a.contentVersion && it.skillId == a.skillId
        } == true }
    }

    fun calculate(skills: List<SkillEntity>, questions: List<QuestionEntity>, attempts: List<QuestionAttemptEntity>): List<SkillMastery> {
        val valid = validAttempts(questions, attempts)
        return skills.map { skill ->
            val recent = valid.filter { it.skillId == skill.id }.takeLast(20)
            val weights = recent.indices.map { (it + 1).toDouble() }
            val score = if (recent.isEmpty()) 0 else (100 * recent.indices.sumOf {
                if (recent[it].correct) weights[it] else 0.0
            } / weights.sum()).roundToInt()
            val distinct = recent.map { it.questionId }.distinct().size
            val mediumOrHard = recent.any { it.correct && it.difficulty >= 2 }
            val status = when {
                recent.isEmpty() -> MasteryStatus.NEW
                distinct < 5 -> MasteryStatus.INSUFFICIENT
                score >= 80 && mediumOrHard -> MasteryStatus.MASTERED
                else -> MasteryStatus.LEARNING
            }
            SkillMastery(skill, score, distinct, questions.count { it.skillId == skill.id },
                recent.size, recent.count { !it.correct }, status)
        }
    }
}

data class QuestionChoice(val question: QuestionEntity, val reason: String)

object AdaptiveQuestionSelector {
    fun choose(
        questions: List<QuestionEntity>, skills: List<SkillEntity>,
        history: List<QuestionAttemptEntity>, sessionAttempts: List<QuestionAttemptEntity>
    ): QuestionChoice? {
        val used = sessionAttempts.map { it.questionId }.toSet()
        val candidates = questions.filter { it.skillId != null && it.id !in used &&
            it.options.size >= 2 && it.correctOptionIndex in it.options.indices }
        if (candidates.isEmpty()) return null
        val mastery = MasteryCalculator.calculate(skills, questions, history).associateBy { it.skill.id }
        val valid = MasteryCalculator.validAttempts(questions, history)
        val available = candidates.groupBy { it.skillId!! }
        // Every fifth question explores new material; every fifth alternate reviews a strong skill.
        val category = when (sessionAttempts.size % 5) { 3 -> 1; 4 -> 2; else -> 0 }
        val skillId = available.keys.sortedWith(compareBy<Long> { id ->
            val m = mastery[id]
            when (category) {
                1 -> if (m?.recentCount == 0) -1000 else m?.recentCount ?: 0
                2 -> -(m?.score ?: 0)
                else -> if (m?.recentCount == 0) 50 else m?.score ?: 0
            }
        }.thenBy { it }).first()
        val recent = valid.filter { it.skillId == skillId }.takeLast(2)
        val lastDifficulty = recent.lastOrNull()?.difficulty ?: 2
        val targetDifficulty = when {
            recent.size == 2 && recent.all { it.correct } -> (lastDifficulty + 1).coerceAtMost(3)
            recent.size == 2 && recent.none { it.correct } -> (lastDifficulty - 1).coerceAtLeast(1)
            else -> lastDifficulty
        }
        val latest = valid.groupBy { it.questionId }.mapValues { it.value.last() }
        val question = available.getValue(skillId).sortedWith(
            compareBy<QuestionEntity> { kotlin.math.abs(it.difficulty - targetDifficulty) }
                .thenBy { q -> when { latest[q.id]?.let { !it.correct && it.confidence == 3 } == true -> 0
                    latest[q.id]?.correct == false -> 1
                    latest[q.id]?.let { it.correct && it.confidence == 1 } == true -> 2
                    latest[q.id] == null -> 3
                    else -> 4 } }
                .thenBy { latest[it.id]?.answeredAt ?: 0 }.thenBy { it.id }
        ).first()
        val name = mastery[skillId]?.skill?.name ?: "kỹ năng này"
        val reason = when {
            recent.size == 2 && recent.none { it.correct } && targetDifficulty < lastDifficulty -> "Bạn vừa sai hai câu $name. Thử luyện lại ở mức dễ hơn."
            recent.size == 2 && recent.none { it.correct } -> "Bạn vừa sai hai câu $name. Củng cố lại kiến thức ở mức Dễ."
            recent.size == 2 && recent.all { it.correct } && targetDifficulty > lastDifficulty -> "Bạn làm tốt hai câu $name. Thử một câu khó hơn."
            latest[question.id]?.let { !it.correct && it.confidence == 3 } == true -> "Bạn từng rất chắc nhưng trả lời sai câu này. Kiểm tra lại cách hiểu về $name."
            mastery[skillId]?.recentCount == 0 -> "Khám phá $name để có dữ liệu đánh giá ban đầu."
            category == 2 -> "Kiểm tra lại $name để củng cố kiến thức đã học."
            else -> "Ưu tiên $name dựa trên kết quả gần đây của bạn."
        } + if (question.difficulty != targetDifficulty) " Ngân hàng hiện có chưa đủ mức khó phù hợp; dùng mức gần nhất." else ""
        return QuestionChoice(question, reason)
    }
}
