package com.example.learning

import com.example.data.local.*
import org.junit.Assert.*
import org.junit.Test

class AdaptiveEngineTest {
    private val skills = listOf(SkillEntity(1, "Android", "Room"), SkillEntity(2, "Android", "Activity"))
    private fun question(id: Long, skill: Long = 1, level: Int = 2, version: Int = 1) =
        QuestionEntity(id, 1, "Question $id", listOf("Yes", "No"), 0, skill, level, version)
    private fun attempt(id: Long, correct: Boolean, skill: Long = 1, level: Int = 2, confidence: Int = 2, version: Int = 1, skipped: Boolean = false) =
        QuestionAttemptEntity(id = id, sessionId = 1, userKey = "student", questionId = id, contentVersion = version,
            skillId = skill, difficulty = level, correct = correct, confidence = confidence, skipped = skipped, answeredAt = id)

    @Test fun repeatingOneQuestionCannotMasterSkill() {
        val q = listOf(question(1))
        val result = MasteryCalculator.calculate(skills, q, (1..20).map { attempt(1, true) }).first()
        assertEquals(100, result.score)
        assertEquals(MasteryStatus.INSUFFICIENT, result.status)
    }

    @Test fun masteryRequiresCoverageAndNonEasyQuestion() {
        val easy = (1L..5L).map { question(it, level = 1) }
        assertEquals(MasteryStatus.LEARNING, MasteryCalculator.calculate(skills, easy,
            easy.map { attempt(it.id, true, level = 1) }).first().status)
        val medium = easy.map { it.copy(difficulty = 2) }
        assertEquals(MasteryStatus.MASTERED, MasteryCalculator.calculate(skills, medium,
            medium.map { attempt(it.id, true) }).first().status)
    }

    @Test fun skippedDeletedAndEditedQuestionsDoNotAffectMastery() {
        val result = MasteryCalculator.calculate(skills, listOf(question(1, version = 2), question(2)),
            listOf(attempt(1, true), attempt(2, false, skipped = true), attempt(3, true))).first()
        assertEquals(MasteryStatus.NEW, result.status)
        assertEquals(0, result.recentCount)
    }

    @Test fun recentAnswersHaveMoreWeight() {
        val q = (1L..5L).map { question(it) }
        val result = MasteryCalculator.calculate(skills, q, q.map { attempt(it.id, it.id >= 4) }).first()
        assertEquals(60, result.score)
    }

    @Test fun weakSkillIsPrioritizedAndUsedQuestionsExcluded() {
        val bank = (1L..6L).map { question(it) } + (7L..12L).map { question(it, 2) }
        val history = listOf(attempt(1, false), attempt(2, false), attempt(7, true, 2), attempt(8, true, 2))
        val next = requireNotNull(AdaptiveQuestionSelector.choose(bank, skills, history, history.take(2)))
        assertEquals(1L, next.question.skillId)
        assertFalse(next.question.id in listOf(1L, 2L))
    }

    @Test fun twoSuccessesRaiseDifficultyAndTwoFailuresLowerIt() {
        val bank = listOf(question(1), question(2), question(3, level = 3), question(4, level = 1))
        val correct = listOf(attempt(1, true), attempt(2, true))
        assertEquals(3, AdaptiveQuestionSelector.choose(bank, skills, correct, correct)?.question?.difficulty)
        val wrong = listOf(attempt(1, false), attempt(2, false))
        assertEquals(1, AdaptiveQuestionSelector.choose(bank, skills, wrong, wrong)?.question?.difficulty)
    }

    @Test fun confidentMistakeIsPrioritizedWithinDifficulty() {
        val bank = listOf(question(1), question(2), question(3))
        val history = listOf(attempt(1, false, confidence = 3))
        assertEquals(1L, AdaptiveQuestionSelector.choose(bank, skills, history, emptyList())?.question?.id)
    }

    @Test fun exhaustedOrInvalidBankEndsSession() {
        assertNull(AdaptiveQuestionSelector.choose(listOf(question(1)), skills, emptyList(), listOf(attempt(1, true))))
        assertNull(AdaptiveQuestionSelector.choose(listOf(question(1).copy(correctOptionIndex = 5)), skills, emptyList(), emptyList()))
    }

    @Test fun fourthQuestionExploresUnstudiedSkill() {
        val bank = (1L..6L).map { question(it) } + listOf(question(7, 2))
        val history = (1L..3L).map { attempt(it, false) }
        assertEquals(2L, AdaptiveQuestionSelector.choose(bank, skills, history, history)?.question?.skillId)
    }
}
