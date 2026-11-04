package com.example.learning

import com.example.data.local.QuestionEntity
import com.example.data.local.ReviewProgressEntity

data class ReviewSummary(val questionCount: Int, val dueCount: Int) {
    companion object {
        fun eligible(question: QuestionEntity) =
            question.options.size >= 2 && question.correctOptionIndex in question.options.indices

        fun current(question: QuestionEntity, progress: ReviewProgressEntity?, now: Long) =
            progress?.let { ReviewScheduler.current(it, question.contentVersion, now) }

        fun calculate(questions: List<QuestionEntity>, progress: List<ReviewProgressEntity>, now: Long): ReviewSummary {
            val byId = progress.associateBy { it.questionId }
            val valid = questions.filter(::eligible)
            return ReviewSummary(valid.size, valid.count { question ->
                current(question, byId[question.id], now)?.nextReviewAt?.let { it <= now } == true
            })
        }
    }
}
