package com.example.learning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LearningRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: LearningRepository
    private val user = "student"
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = LearningRepository(db)
    }
    @After fun close() { db.close() }

    @Test fun duplicateAnswerIsIgnoredAndOtherUserCannotWriteSession() = runTest {
        repo.installDemo()
        val id = repo.start(user, 5, null, null, null)
        val s = requireNotNull(db.learningDao().session(id, user))
        repo.answer(user, id, s.currentQuestionId, s.currentVersion, 0, 3, 5000)
        repo.answer(user, id, s.currentQuestionId, s.currentVersion, 1, 1, 10)
        repo.answer("other", id, s.currentQuestionId, s.currentVersion, 1, 1, 10)
        assertEquals(1, db.learningDao().attempts(user).size)
        assertTrue(db.learningDao().attempts(user).single().correct)
        assertTrue(db.learningDao().attempts("other").isEmpty())
    }

    @Test fun skipDoesNotCountAsWrongAndStaleClickDoesNotSkipNextQuestion() = runTest {
        repo.installDemo()
        val id = repo.start(user, 5, null, null, null)
        val old = requireNotNull(db.learningDao().session(id, user))
        repo.next(user, id, old.currentQuestionId, old.currentVersion, skip = true)
        val next = requireNotNull(db.learningDao().session(id, user))
        assertNotEquals(old.currentQuestionId, next.currentQuestionId)
        repo.next(user, id, old.currentQuestionId, old.currentVersion, skip = true)
        assertEquals(next.currentQuestionId, db.learningDao().session(id, user)?.currentQuestionId)
        val mastery = MasteryCalculator.calculate(db.learningDao().skills(), db.learningDao().questions(), db.learningDao().attempts(user))
        assertTrue(mastery.all { it.recentCount == 0 })
    }

    @Test fun sessionProgressAndTimingAreStoredAndSingleUnfinishedSessionIsEnforced() = runTest {
        repo.installDemo()
        val id = repo.start(user, 5, null, null, null)
        val s = requireNotNull(db.learningDao().session(id, user))
        repo.saveElapsed(user, id, requireNotNull(s.currentQuestionId), s.currentVersion, 7000)
        assertEquals(7000L, db.learningDao().unfinished(user)?.accumulatedMillis)
        var rejected = false
        try { repo.start(user, 5, null, null, null) } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
        repo.finish(user, id)
        assertNull(db.learningDao().unfinished(user))
    }

    @Test fun demoIsIdempotentAndClassificationResetsOnlySelectedEvidence() = runTest {
        repo.installDemo(); repo.installDemo()
        assertEquals(18, db.learningDao().questions().size)
        assertEquals(3, db.learningDao().skills().size)
        val id = repo.start(user, 5, null, null, null)
        val s = requireNotNull(db.learningDao().session(id, user))
        repo.answer(user, id, s.currentQuestionId, s.currentVersion, 0, 3, 100)
        val q = db.learningDao().questions().first { it.id == s.currentQuestionId }
        repo.classify(listOf(q.id), "Android", "Room", 3)
        val updated = db.learningDao().questions().first { it.id == q.id }
        assertEquals(q.contentVersion + 1, updated.contentVersion)
        assertTrue(MasteryCalculator.validAttempts(db.learningDao().questions(), db.learningDao().attempts(user)).isEmpty())
    }

    @Test fun fiveAnswersCompleteSessionAndBankCanEndEarly() = runTest {
        repo.installDemo()
        val id = repo.start(user, 5, null, null, null)
        repeat(5) {
            val s = requireNotNull(db.learningDao().session(id, user))
            repo.answer(user, id, s.currentQuestionId, s.currentVersion, 0, 2, 10)
            repo.next(user, id, s.currentQuestionId, s.currentVersion)
        }
        assertNotNull(db.learningDao().session(id, user)?.completedAt)
        assertEquals(5, db.learningDao().attempts(user).count { !it.skipped })
        val q = db.learningDao().questions().first()
        repo.classify(listOf(q.id), "Test", "Only one", 2)
        val skill = requireNotNull(db.learningDao().findSkill("Test", "Only one"))
        val shortId = repo.start(user, 10, null, null, skill.id)
        val s = requireNotNull(db.learningDao().session(shortId, user))
        repo.next(user, shortId, s.currentQuestionId, s.currentVersion, skip = true)
        assertNotNull(db.learningDao().session(shortId, user)?.completedAt)
    }

    @Test fun checkedAnswerSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "learning-resume-test.db"
        db.close()
        context.deleteDatabase(name)
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        repo = LearningRepository(db)
        repo.installDemo()
        val id = repo.start(user, 5, null, null, null)
        val s = requireNotNull(db.learningDao().session(id, user))
        repo.answer(user, id, s.currentQuestionId, s.currentVersion, 0, 3, 5000)
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        repo = LearningRepository(db)
        val restored = requireNotNull(db.learningDao().unfinished(user))
        assertEquals(s.currentQuestionId, restored.currentQuestionId)
        assertTrue(restored.answered)
        assertEquals(0, restored.selectedOption)
        assertEquals(5000L, db.learningDao().attempts(user).single().activeMillis)
        repo.next(user, id, restored.currentQuestionId, restored.currentVersion)
        assertNotEquals(restored.currentQuestionId, db.learningDao().session(id, user)?.currentQuestionId)
        db.close()
        context.deleteDatabase(name)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
}
