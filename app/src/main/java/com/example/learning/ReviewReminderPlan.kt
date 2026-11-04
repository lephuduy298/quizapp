package com.example.learning

import com.example.data.local.QuestionEntity
import com.example.data.local.ReviewProgressEntity

data class ReviewReminderPlan(val dueCount: Int, val dueKeys: Set<String>,
    val activeKeys: Set<String>, val nextAlarmAt: Long?) {
    companion object {
        fun create(questions: List<QuestionEntity>, progress: List<ReviewProgressEntity>, now: Long): ReviewReminderPlan {
            val bank = questions.associateBy { it.id }
            val scheduled = progress.mapNotNull { original ->
                val question = bank[original.questionId]?.takeIf(ReviewSummary::eligible) ?: return@mapNotNull null
                val originalDue = original.nextReviewAt ?: return@mapNotNull null
                val current = ReviewScheduler.current(original, question.contentVersion, now)
                // Stable even when an edit makes the card immediately due on every read.
                "${question.id}:${question.contentVersion}:$originalDue" to requireNotNull(current.nextReviewAt)
            }
            val due = scheduled.filter { it.second <= now }.map { it.first }.toSet()
            return ReviewReminderPlan(due.size, due, scheduled.map { it.first }.toSet(),
                scheduled.map { it.second }.filter { it > now }.minOrNull())
        }
    }
}
