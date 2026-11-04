package com.example.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.room.InvalidationTracker
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.UserManager
import com.example.data.repository.LearningRepository
import com.example.learning.ReviewReminderPlan
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One alarm for the next review deadline; observe only committed schedule changes. */
object ReviewDueReminder {
    const val ACTION_DUE = "com.example.reminder.REVIEW_DUE"
    const val CHANNEL_ID = "review_due_channel"
    const val NOTIFICATION_ID = 1002
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var observedApp: Context? = null
    private var observerJob: Job? = null

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL_ID, "Lịch ôn lặp lại ngắt quãng",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Nhắc khi các câu hỏi đã đến lịch ôn lại"
            })
    }

    fun canNotify(context: Context): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    @Synchronized fun start(context: Context) {
        val app = context.applicationContext
        if (observedApp === app && observerJob?.isActive == true) return
        observerJob?.cancel()
        observedApp = app
        observerJob = scope.launch {
            val preferences = callbackFlow {
                val prefs = app.getSharedPreferences("user_auth_prefs", Context.MODE_PRIVATE)
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "current_user" || key?.startsWith("review_due_enabled_") == true) trySend(Unit)
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                trySend(Unit)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val db = AppDatabase.getDatabase(app)
            val changes = callbackFlow {
                val observer = object : InvalidationTracker.Observer("questions", "review_progress") {
                    override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
                }
                db.invalidationTracker.addObserver(observer)
                // Register before the initial reconciliation so early writes are not missed.
                trySend(Unit)
                awaitClose { db.invalidationTracker.removeObserver(observer) }
            }
            merge(preferences, changes).debounce(200).collect { refresh(app).join() }
        }
    }

    fun refresh(context: Context): Job = scope.launch {
        mutex.withLock {
            try {
                ReviewReminderController(context.applicationContext, AppDatabase.getDatabase(context),
                    UserManager.getInstance(context)).refresh()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.e("ReviewDueReminder", "Unable to refresh review reminder", e) }
        }
    }
}

/** Re-read current state at delivery, rather than trusting a stale alarm payload. */
class ReviewReminderController(private val context: Context, private val db: AppDatabase,
    private val userManager: UserManager, private val clock: () -> Long = { System.currentTimeMillis() }) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences("review_due_reminders", Context.MODE_PRIVATE)

    private fun pendingAlarm() = PendingIntent.getBroadcast(context, 2027,
        Intent(context, ReviewDueReminderReceiver::class.java).setAction(ReviewDueReminder.ACTION_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    suspend fun refresh() {
        val user = userManager.getCurrentUser()?.let(LearningRepository::userKey)
        if (user == null || !userManager.isReviewReminderEnabled()) {
            alarms.cancel(pendingAlarm())
            notifications.cancel(ReviewDueReminder.NOTIFICATION_ID)
            return
        }
        val now = clock()
        val plan = db.withTransaction {
            ReviewReminderPlan.create(db.learningDao().questions(), db.reviewDao().allProgress(user), now)
        }
        // Do not publish an old account's snapshot after logout/switch during the Room read.
        if (userManager.getCurrentUser()?.let(LearningRepository::userKey) != user ||
            !userManager.isReviewReminderEnabled()) return

        val ledgerKey = "notified:$user"
        val seen = prefs.getStringSet(ledgerKey, emptySet()).orEmpty().intersect(plan.activeKeys)
        var notified = seen
        if (plan.dueCount == 0) notifications.cancel(ReviewDueReminder.NOTIFICATION_ID)
        else if (ReviewDueReminder.canNotify(context)) {
            val hasNew = (plan.dueKeys - seen).isNotEmpty()
            val visible = notifications.activeNotifications.any { it.id == ReviewDueReminder.NOTIFICATION_ID }
            if (hasNew || visible) {
                val contentIntent = PendingIntent.getActivity(context, 2027,
                    Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_REVIEW_REMINDER
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val message = "Bạn có ${plan.dueCount} câu đến lịch ôn. Chạm để ôn ngay."
                val notification = NotificationCompat.Builder(context, ReviewDueReminder.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle("Đến lịch ôn luyện rồi! 📚")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setNumber(plan.dueCount)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build()
                try {
                    notifications.notify(ReviewDueReminder.NOTIFICATION_ID, notification)
                    notified = seen + plan.dueKeys
                } catch (e: SecurityException) {
                    Log.w("ReviewDueReminder", "Notification permission was revoked", e)
                }
            }
        }
        // Durable deduplication, bounded to identities still present in the current schedule.
        prefs.edit().putStringSet(ledgerKey, notified).commit()
        alarms.cancel(pendingAlarm())
        plan.nextAlarmAt?.let { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pendingAlarm()) }
    }
}

class ReviewDueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ReviewDueReminder.ACTION_DUE, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pending = goAsync()
        ReviewDueReminder.refresh(context).invokeOnCompletion { pending.finish() }
    }
}
