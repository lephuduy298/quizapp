package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyWidgetSummaryTest {
    @Test fun summaryUsesCurrentContentAndOnlyTheRequestedAccountWithoutWritingProgress() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val quizId = db.quizDao().insertQuiz(QuizEntity(title = "Widget test", topic = "Test", durationMinutes = 5))
            suspend fun add(text: String, options: List<String> = listOf("A", "B")): QuestionEntity {
                val q = QuestionEntity(quizId = quizId, text = text, options = options, correctOptionIndex = 0)
                return q.copy(id = db.quizDao().insertQuestion(q))
            }
            val due = add("Due")
            val future = add("Future")
            val edited = add("Edited")
            add("New")
            val invalid = add("Invalid", listOf("A"))
            val deleted = add("Deleted")
            val dao = db.reviewDao()
            for (q in listOf(due, future, edited, invalid, deleted)) {
                dao.save(ReviewProgressEntity("alice", q.id, 1,
                    nextReviewAt = if (q == future || q == edited) 2000 else 1000))
            }
            db.quizDao().updateQuestion(edited.copy(contentVersion = 2))
            db.quizDao().deleteQuestion(deleted)
            val repo = ReviewRepository(db) { 1000L }
            assertEquals(ReviewSummary(4, 2), repo.summary(" Alice "))
            assertEquals(ReviewSummary(4, 0), repo.summary("bob"))
            // Repeated widget reads must not reset the stored schedule or create evidence.
            assertEquals(ReviewSummary(4, 2), repo.summary("alice"))
            assertEquals(2000L, dao.progress("alice", edited.id)?.nextReviewAt)
            assertEquals(1, dao.progress("alice", edited.id)?.contentVersion)
            assertEquals(4, dao.allProgress("alice").size)
            assertEquals(ReviewSummary(4, 3), repo.summary("alice", 2000))
        } finally { db.close() }
    }
}
