package com.example.learning

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.MainActivity
import com.example.data.local.*
import com.example.data.repository.ReviewRepository
import com.example.reminder.ReviewDueReminder
import com.example.reminder.ReviewReminderController
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReviewDueReminderTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var manager: UserManager
    private lateinit var alarms: AlarmManager
    private lateinit var notifications: NotificationManager
    private var now = 1_000_000L
    private lateinit var controller: ReviewReminderController

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        manager = UserManager.getInstance(context)
        login("student")
        manager.setReviewReminderEnabled(true)
        alarms = context.getSystemService(AlarmManager::class.java)
        notifications = context.getSystemService(NotificationManager::class.java)
        controller = ReviewReminderController(context, db, manager) { now }
    }

    @After fun close() { db.close(); manager.logout() }

    private fun login(user: String) {
        manager.register(user, "testpassword")
        check(manager.login(user, "testpassword"))
    }

    private suspend fun card(due: Long?, user: String = "student"): QuestionEntity {
        val quiz = db.quizDao().insertQuiz(QuizEntity(title = "Đề", topic = "Android", durationMinutes = 5))
        val q = QuestionEntity(quizId = quiz, text = "Câu hỏi $quiz", options = listOf("A", "B"), correctOptionIndex = 0)
        val saved = q.copy(id = db.quizDao().insertQuestion(q))
        db.reviewDao().save(ReviewProgressEntity(user, saved.id, 1, nextReviewAt = due))
        return saved
    }

    private fun scheduledAt(): Long? = shadowOf(alarms).scheduledAlarms
        .filter { it.operation != null && shadowOf(it.operation).savedIntent.action == ReviewDueReminder.ACTION_DUE }
        .singleOrNull()?.triggerAtTime

    private fun notice(): Notification? = shadowOf(notifications).getNotification(ReviewDueReminder.NOTIFICATION_ID)

    @Test fun waitsForDeadlineThenGroupsDueCardsAndSchedulesTheNextAlarm() = runBlocking {
        card(now + 600_000)
        card(now + 600_000)
        card(now + 86_400_000)
        controller.refresh()
        assertNull(notice())
        assertEquals(now + 600_000, scheduledAt())
        now += 600_000
        controller.refresh()
        val notification = requireNotNull(notice())
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("2 câu"))
        assertEquals(MainActivity.ACTION_REVIEW_REMINDER, shadowOf(notification.contentIntent).savedIntent.action)
        assertEquals(86_400_000L + 1_000_000L, scheduledAt())
    }

    @Test fun dismissedNotificationsStayDeduplicatedAcrossControllerRecreationAndNewReviewsCanNotify() = runBlocking {
        val q = card(now)
        controller.refresh()
        assertNotNull(notice())
        notifications.cancel(ReviewDueReminder.NOTIFICATION_ID)
        ReviewReminderController(context, db, manager) { now }.refresh()
        assertNull(notice())
        ReviewRepository(db) { now }.rate("student", q.id, 1, ReviewRating.AGAIN, "next-review")
        controller.refresh()
        assertEquals(now + 600_000, scheduledAt())
        now += 600_000
        controller.refresh()
        assertNotNull(notice())
    }

    @Test fun logoutAccountSwitchAndTurningOffCancelTheOldUsersAlarmAndNotification() = runBlocking {
        card(now)
        card(now + 600_000)
        controller.refresh()
        assertNotNull(notice())
        manager.logout()
        controller.refresh()
        assertNull(notice())
        assertNull(scheduledAt())
        login("other")
        assertFalse(manager.isReviewReminderEnabled())
        manager.setReviewReminderEnabled(true)
        controller.refresh()
        assertNull(notice())
        assertNull(scheduledAt())
        login("student")
        controller.refresh()
        assertNotNull(scheduledAt())
        manager.setReviewReminderEnabled(false)
        controller.refresh()
        assertNull(scheduledAt())
        assertNull(notice())
    }

    @Test fun persistedScheduleCanBeRestoredAfterSystemAlarmsAreCleared() = runBlocking {
        card(now + 600_000)
        controller.refresh()
        shadowOf(alarms).scheduledAlarms.toList().forEach { it.operation?.let(alarms::cancel) }
        assertNull(scheduledAt())
        ReviewReminderController(context, db, manager) { now }.refresh()
        assertEquals(now + 600_000, scheduledAt())
        assertNull(notice())
    }

    @Test fun deniedPermissionDoesNotConsumeTheNotificationAndGrantingItRetries() = runBlocking {
        card(now)
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        controller.refresh()
        assertNull(notice())
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        controller.refresh()
        assertNotNull(notice())
    }

    @Test fun editedAndDeletedCardsAreReconciledAndNotesWithoutAScheduleDoNotNotify() = runBlocking {
        val q = card(now + 600_000)
        card(null)
        controller.refresh()
        db.quizDao().updateQuestion(q.copy(contentVersion = 2))
        controller.refresh()
        assertNotNull(notice())
        assertNull(scheduledAt())
        notifications.cancel(ReviewDueReminder.NOTIFICATION_ID)
        now += 1000
        controller.refresh()
        assertNull(notice())
        db.quizDao().deleteQuestion(q)
        controller.refresh()
        assertNull(notice())
        assertNull(scheduledAt())
    }
}
