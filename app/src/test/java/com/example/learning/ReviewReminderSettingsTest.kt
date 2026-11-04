package com.example.learning

import android.Manifest
import android.app.AlarmManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.QuizApplication
import com.example.data.local.*
import com.example.reminder.ReviewDueReminder
import com.example.ui.screens.ReviewReminderSettings
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewReminderSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toggleSchedulesPersistedReviewsAndCommittedChangesRescheduleWithoutManualRefresh() {
        val app = ApplicationProvider.getApplicationContext<QuizApplication>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.container.userManager
        manager.register("remindersettings", "testpassword")
        check(manager.login("remindersettings", "testpassword"))
        manager.setReviewReminderEnabled(false)
        val db = AppDatabase.getDatabase(app)
        val deadline = System.currentTimeMillis() + 600_000
        val q = runBlocking {
            val quiz = db.quizDao().insertQuiz(QuizEntity(title = "Đề nhắc ôn", topic = "Android", durationMinutes = 5))
            val id = db.quizDao().insertQuestion(QuestionEntity(quizId = quiz, text = "Câu ôn",
                options = listOf("A", "B"), correctOptionIndex = 0))
            db.reviewDao().save(ReviewProgressEntity("remindersettings", id, 1, nextReviewAt = deadline))
            id
        }
        val alarms = app.getSystemService(AlarmManager::class.java)
        fun scheduledAt() = shadowOf(alarms).scheduledAlarms.filter {
            it.operation != null && shadowOf(it.operation).savedIntent.action == ReviewDueReminder.ACTION_DUE
        }.singleOrNull()?.triggerAtTime

        compose.setContent { MyApplicationTheme { ReviewReminderSettings() } }
        compose.onNodeWithTag("review_reminder_toggle").assertIsOff().performClick().assertIsOn()
        assertTrue(manager.isReviewReminderEnabled())
        compose.waitUntil(15000) { compose.waitForIdle(); scheduledAt() == deadline }

        val later = deadline + 86_400_000
        runBlocking {
            db.reviewDao().save(requireNotNull(db.reviewDao().progress("remindersettings", q)).copy(nextReviewAt = later))
        }
        compose.waitUntil(15000) { compose.waitForIdle(); scheduledAt() == later }

        compose.onNodeWithTag("review_reminder_toggle").performClick().assertIsOff()
        compose.waitUntil(15000) { compose.waitForIdle(); scheduledAt() == null }
        assertFalse(manager.isReviewReminderEnabled())
        manager.logout()
    }
}
