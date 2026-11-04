package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewMigrationTest {
    @Test fun versionThreeMigrationPreservesLearningAndSeedsOnlyOwnedCurrentEvidence() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "review-migration-test.db"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        LearningRepository(original).installDemo()
        val q = original.learningDao().questions().first()
        val dao = original.learningDao()
        dao.insertAttempt(QuestionAttemptEntity(sessionId = 1, userKey = "student", questionId = q.id,
            contentVersion = 1, skillId = q.skillId!!, difficulty = 1, correct = false, selectedOption = 1, answeredAt = 100))
        dao.insertAttempt(QuestionAttemptEntity(sessionId = 2, userKey = "student", questionId = q.id,
            contentVersion = 1, skillId = q.skillId!!, difficulty = 1, correct = true, answeredAt = 200))
        dao.insertAttempt(QuestionAttemptEntity(sessionId = 3, userKey = "other", questionId = q.id,
            contentVersion = 1, skillId = q.skillId!!, difficulty = 1, correct = false, skipped = true))
        dao.insertAttempt(QuestionAttemptEntity(sessionId = 4, userKey = "edited", questionId = q.id,
            contentVersion = 0, skillId = q.skillId!!, difficulty = 1, correct = false))
        original.close()
        // Remove only the new tables to reconstruct the exact v3 schema on disk.
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use {
            it.execSQL("DROP TABLE review_events")
            it.execSQL("DROP TABLE review_progress")
            it.execSQL("DROP TABLE room_master_table")
            it.version = 3
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_3_4).build()
        try {
            assertEquals(18, migrated.learningDao().questions().size)
            assertEquals(4, migrated.learningDao().attempts("student").size + migrated.learningDao().attempts("other").size + migrated.learningDao().attempts("edited").size)
            val p = requireNotNull(migrated.reviewDao().progress("student", q.id))
            assertEquals(2, p.reviewCount)
            assertEquals(1, p.mistakeCount)
            assertEquals(0L, p.nextReviewAt)
            assertNull(migrated.reviewDao().progress("other", q.id))
            assertNull(migrated.reviewDao().progress("edited", q.id))
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
