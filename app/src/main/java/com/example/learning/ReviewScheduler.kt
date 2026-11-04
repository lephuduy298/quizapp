package com.example.learning

import com.example.data.local.ReviewProgressEntity

enum class ReviewRating(val label: String) {
    AGAIN("Chưa nhớ"), HARD("Khó"), GOOD("Nhớ"), EASY("Dễ")
}

object ReviewScheduler {
    const val DAY = 86_400_000L
    private val intervals = listOf(1, 3, 7, 14, 30)

    fun rate(progress: ReviewProgressEntity, rating: ReviewRating, now: Long): ReviewProgressEntity {
        val stage = when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> maxOf(1, progress.stage)
            ReviewRating.GOOD -> progress.stage + 1
            ReviewRating.EASY -> progress.stage + 2
        }.coerceIn(0, intervals.size)
        val delay = when (rating) {
            ReviewRating.AGAIN -> 10 * 60_000L
            ReviewRating.HARD -> DAY
            else -> intervals[stage - 1] * DAY
        }
        return progress.copy(stage = stage, nextReviewAt = now + delay,
            lastReviewedAt = now, reviewCount = progress.reviewCount + 1)
    }

    fun current(progress: ReviewProgressEntity, version: Int, now: Long): ReviewProgressEntity =
        if (progress.contentVersion == version) progress else ReviewProgressEntity(
            userKey = progress.userKey, questionId = progress.questionId, contentVersion = version,
            nextReviewAt = if (progress.nextReviewAt != null) now else null,
            note = progress.note, isFavorite = progress.isFavorite)
}
