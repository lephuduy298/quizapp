package com.example.learning

import com.example.data.local.ReviewProgressEntity
import org.junit.Assert.*
import org.junit.Test

class ReviewSchedulerTest {
    private val initial = ReviewProgressEntity("student", 1, 1)
    @Test fun successfulReviewsExpandAndCapTheInterval() {
        var p = initial
        listOf(1, 3, 7, 14, 30, 30).forEachIndexed { index, days ->
            p = ReviewScheduler.rate(p, ReviewRating.GOOD, 1000)
            assertEquals(1000 + days * ReviewScheduler.DAY, p.nextReviewAt)
            assertEquals(index + 1, p.reviewCount)
        }
        assertEquals(5, p.stage)
    }
    @Test fun forgettingResetsAndHardDoesNotAdvanceAnEstablishedStage() {
        val mature = initial.copy(stage = 4, reviewCount = 8)
        val hard = ReviewScheduler.rate(mature, ReviewRating.HARD, 1000)
        assertEquals(4, hard.stage)
        assertEquals(1000 + ReviewScheduler.DAY, hard.nextReviewAt)
        val again = ReviewScheduler.rate(mature, ReviewRating.AGAIN, 1000)
        assertEquals(0, again.stage)
        assertEquals(601000L, again.nextReviewAt)
        assertEquals(1000 + ReviewScheduler.DAY, ReviewScheduler.rate(again, ReviewRating.GOOD, 1000).nextReviewAt)
    }
    @Test fun easyAdvancesFasterAndEditingPreservesOnlyAnnotations() {
        assertEquals(3 * ReviewScheduler.DAY, ReviewScheduler.rate(initial, ReviewRating.EASY, 0).nextReviewAt)
        val old = initial.copy(stage = 5, nextReviewAt = 9999, mistakeCount = 3, note = "My note", isFavorite = true)
        val changed = ReviewScheduler.current(old, 2, 100)
        assertEquals(100L, changed.nextReviewAt)
        assertEquals(0, changed.mistakeCount)
        assertEquals(0, changed.stage)
        assertEquals("My note", changed.note)
        assertTrue(changed.isFavorite)
        assertNull(ReviewScheduler.current(initial.copy(note = "Note"), 2, 100).nextReviewAt)
    }
}
