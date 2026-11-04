package com.example.learning

import com.example.data.local.QuestionEntity
import com.example.data.local.ReviewProgressEntity
import org.junit.Assert.*
import org.junit.Test

class ReviewReminderPlanTest {
    private fun question(id: Long, version: Int = 1) = QuestionEntity(id, 1, "Câu $id", listOf("A", "B"), 0,
        contentVersion = version)
    private fun progress(id: Long, due: Long?, version: Int = 1) = ReviewProgressEntity("student", id, version,
        nextReviewAt = due)

    @Test fun groupsDueCardsAndChoosesOnlyTheEarliestFutureDeadline() {
        val plan = ReviewReminderPlan.create((1L..4L).map { question(it) },
            listOf(progress(1, 100), progress(2, 200), progress(3, 400), progress(4, 300)), 200)
        assertEquals(2, plan.dueCount)
        assertEquals(setOf("1:1:100", "2:1:200"), plan.dueKeys)
        assertEquals(300L, plan.nextAlarmAt)
    }

    @Test fun ignoresUnscheduledDeletedAndInvalidQuestions() {
        val plan = ReviewReminderPlan.create(listOf(question(1), question(2).copy(correctOptionIndex = 10)),
            listOf(progress(1, null), progress(2, 100), progress(3, 100)), 200)
        assertEquals(0, plan.dueCount)
        assertTrue(plan.activeKeys.isEmpty())
        assertNull(plan.nextAlarmAt)
    }

    @Test fun editedQuestionIsDueNowButItsDeduplicationKeyDoesNotChangeWithTheClock() {
        val bank = listOf(question(1, version = 2))
        val history = listOf(progress(1, 1000))
        val first = ReviewReminderPlan.create(bank, history, 100)
        val later = ReviewReminderPlan.create(bank, history, 200)
        assertEquals(1, first.dueCount)
        assertEquals(first.dueKeys, later.dueKeys)
        assertEquals(setOf("1:2:1000"), first.dueKeys)
        assertNull(first.nextAlarmAt)
    }
}
