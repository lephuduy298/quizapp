package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ReviewRepository
    private var now = 1000L
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ReviewRepository(db) { now }
    }
    @After fun close() { db.close() }
    private suspend fun question(): QuestionEntity {
        LearningRepository(db).installDemo()
        return db.learningDao().questions().first()
    }
    @Test fun wrongAnswersAreOwnedAndDuplicateEventsCannotAdvanceSchedule() = runTest {
        val q = question()
        assertTrue(repo.recordAnswer(" Student ", q.id, 1, 1, "answer1"))
        now += 1000
        assertTrue(repo.recordAnswer("student", q.id, 1, 1, "answer1"))
        val p = requireNotNull(db.reviewDao().progress("student", q.id))
        assertEquals(1, p.mistakeCount)
        assertEquals(1, p.reviewCount)
        assertEquals(601000L, p.nextReviewAt)
        assertEquals(1, p.lastSelectedOption)
        assertTrue(repo.progress("other").first().isEmpty())
        assertTrue(repo.rate("other", q.id, 1, ReviewRating.GOOD, "answer1"))
        assertEquals(1, db.reviewDao().progress("other", q.id)?.reviewCount)
    }
    @Test fun flashcardMemoryDoesNotInventWrongAnswersAndAnnotationsDoNotCreateSchedules() = runTest {
        val q = question()
        repo.annotate("student", q.id, "  Remember this  ", true)
        assertNull(db.reviewDao().progress("student", q.id)?.nextReviewAt)
        repo.rate("student", q.id, 1, ReviewRating.AGAIN, "card1")
        val p = requireNotNull(db.reviewDao().progress("student", q.id))
        assertEquals(0, p.mistakeCount)
        assertEquals("Remember this", p.note)
        assertTrue(p.isFavorite)
    }
    @Test fun editedAndDeletedQuestionsRejectStaleRatingsAndResetEvidence() = runTest {
        val q = question()
        repo.recordAnswer("student", q.id, 1, 1, "wrong")
        repo.annotate("student", q.id, "Note", true)
        db.quizDao().updateQuestion(q.copy(contentVersion = 2))
        assertFalse(repo.rate("student", q.id, 1, ReviewRating.GOOD, "stale"))
        assertTrue(repo.rate("student", q.id, 2, ReviewRating.GOOD, "fresh"))
        val p = requireNotNull(db.reviewDao().progress("student", q.id))
        assertEquals(1, p.reviewCount)
        assertEquals(0, p.mistakeCount)
        assertEquals("Note", p.note)
        assertTrue(p.isFavorite)
        db.quizDao().deleteQuestion(q.copy(contentVersion = 2))
        assertNull(db.reviewDao().progress("student", q.id))
        assertNull(db.reviewDao().event("student", "fresh"))
        assertFalse(repo.rate("student", q.id, 2, ReviewRating.GOOD, "deleted"))
    }
    @Test fun adaptiveAnswerCreatesExactlyOneReviewAndSkippingCreatesNone() = runTest {
        val learning = LearningRepository(db)
        learning.installDemo()
        val id = learning.start("student", 5, null, null, null)
        val s = requireNotNull(db.learningDao().session(id, "student"))
        learning.answer("student", id, s.currentQuestionId, s.currentVersion, 1, 3, 10)
        learning.answer("student", id, s.currentQuestionId, s.currentVersion, 1, 3, 10)
        assertEquals(1, repo.progress("student").first().single().mistakeCount)
        learning.next("student", id, s.currentQuestionId, s.currentVersion)
        val next = requireNotNull(db.learningDao().session(id, "student"))
        learning.next("student", id, next.currentQuestionId, next.currentVersion, skip = true)
        assertEquals(1, repo.progress("student").first().size)
    }
    @Test fun submittingPracticeDoesNotCountAnswersTwiceAndUnansweredQuestionsAreNotMistakes() = runTest {
        val q = question()
        val questions = db.learningDao().questions()
        repo.recordAnswer("student", q.id, 1, 1, "practice:run:${q.id}")
        val session = QuizSessionEntity(quizId = q.quizId, startTime = 1, endTime = 2,
            score = 0.0, correctAnswersCount = 0, totalQuestionsCount = questions.size, answersMap = mapOf(q.id to 1))
        val id = repo.saveQuizSession("student", session, questions, "practice:run")
        assertNotNull(db.quizSessionDao().getSessionById(id))
        assertEquals(1, repo.progress("student").first().single().reviewCount)
        repo.saveQuizSession("student", session, questions, "exam:run")
        assertEquals(2, repo.progress("student").first().single().mistakeCount)
    }
    @Test fun progressAndEventsSurviveDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "review-reopen-test.db"
        context.deleteDatabase(name)
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        repo = ReviewRepository(db) { now }
        val q = question()
        repo.rate("student", q.id, 1, ReviewRating.EASY, "persisted")
        repo.annotate("student", q.id, "Keep", true)
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        repo = ReviewRepository(db) { now }
        assertEquals("Keep", db.reviewDao().progress("student", q.id)?.note)
        repo.rate("student", q.id, 1, ReviewRating.EASY, "persisted")
        assertEquals(1, db.reviewDao().progress("student", q.id)?.reviewCount)
        db.close()
        context.deleteDatabase(name)
    }
}
