package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserManager
import java.util.Calendar

class StudyReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "StudyReminderReceiver"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "study_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action: ${intent.action}")
        val userManager = UserManager.getInstance(context)

        // If system booted or powered on, restore alarm if enabled
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            if (userManager.isReminderEnabled()) {
                val hour = userManager.getReminderHour()
                val minute = userManager.getReminderMinute()
                StudyReminderHelper.scheduleReminder(context, hour, minute)
                Log.d(TAG, "Restored alarm reminder on boot for $hour:$minute")
            }
            return
        }

        if (intent.action == "com.example.reminder.ACTION_SHOW_REMINDER") {
            // Only trigger if enabled and user is logged in
            if (userManager.isReminderEnabled() && userManager.isUserLoggedIn()) {
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val reminderDays = userManager.getReminderDays()

                if (reminderDays.contains(currentDay)) {
                    showNotification(context)
                    Log.d(TAG, "Dispatched notification for day: $currentDay")
                } else {
                    Log.d(TAG, "Skipped notification: current day ($currentDay) not in user selected days ($reminderDays)")
                }

                // Re-schedule alarm for tomorrow to ensure it repeats daily
                val hour = userManager.getReminderHour()
                val minute = userManager.getReminderMinute()
                StudyReminderHelper.scheduleReminder(context, hour, minute)
                Log.d(TAG, "Rescheduled reminder for tomorrow")
            }
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android O (API 26+) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở học tập",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh thông báo nhắc nhở học tập hàng ngày"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch MainActivity when clicking the notification
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // App launcher icon
            .setContentTitle("Giờ học đến rồi! 📚")
            .setContentText("Đã đến lúc làm một bài quiz ngắn để nâng cao kiến thức và giữ vững Streak của bạn! 🔥")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification displayed successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification. Permission might have been revoked.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }
}
