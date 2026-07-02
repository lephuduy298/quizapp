package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UserManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_USER_PREFIX = "user_pass_"

        @Volatile
        private var INSTANCE: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Registers a new user. Returns true if successful, false if user already exists.
     */
    fun register(username: String, password: String): Boolean {
        val sanitized = username.trim().lowercase()
        if (sanitized.isEmpty() || password.isEmpty()) return false
        
        if (prefs.contains(KEY_USER_PREFIX + sanitized)) {
            return false // User already exists!
        }
        
        prefs.edit().putString(KEY_USER_PREFIX + sanitized, password).apply()
        return true
    }

    /**
     * Attempts to log in. Returns true if credentials are correct.
     */
    fun login(username: String, password: String): Boolean {
        val sanitized = username.trim().lowercase()
        if (sanitized.isEmpty() || password.isEmpty()) return false

        val storedPassword = prefs.getString(KEY_USER_PREFIX + sanitized, null)
        if (storedPassword == password) {
            prefs.edit().putString(KEY_CURRENT_USER, username).apply()
            return true
        }
        return false
    }

    /**
     * Logs the current user out.
     */
    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
    }

    /**
     * Gets the currently authenticated user's name.
     */
    fun getCurrentUser(): String? {
        return prefs.getString(KEY_CURRENT_USER, null)
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Gets the current streak count for the authenticated user.
     */
    fun getStreakCount(): Int {
        val user = getCurrentUser() ?: "guest"
        return prefs.getInt("streak_count_$user", 0)
    }

    /**
     * Gets the current active streak count. If the streak is broken (more than 1 day of inactivity), returns 0.
     */
    fun getActiveStreakCount(): Int {
        val user = getCurrentUser() ?: "guest"
        val lastDate = getLastActiveDate() ?: return 0
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastDate == currentDate) {
            return getStreakCount()
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        try {
            val d1 = sdf.parse(lastDate)
            val d2 = sdf.parse(currentDate)
            if (d1 != null && d2 != null) {
                val diffTime = d2.time - d1.time
                val diffDays = diffTime / (1000 * 60 * 60 * 24)
                if (diffDays <= 1L) {
                    return getStreakCount()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return 0
    }

    /**
     * Gets the last active date (yyyy-MM-dd) string.
     */
    fun getLastActiveDate(): String? {
        val user = getCurrentUser() ?: "guest"
        return prefs.getString("streak_last_date_$user", null)
    }

    /**
     * Updates/advances the streak if active today.
     * Returns true if the streak count was updated.
     */
    fun updateStreak(): Boolean {
        val user = getCurrentUser() ?: "guest"
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = getLastActiveDate()
        val currentStreak = getStreakCount()

        if (lastDate == null) {
            // First day active ever
            prefs.edit()
                .putInt("streak_count_$user", 1)
                .putString("streak_last_date_$user", currentDate)
                .apply()
            return true
        }

        if (lastDate == currentDate) {
            // Already active today, no change
            return false
        }

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        try {
            val d1 = sdf.parse(lastDate)
            val d2 = sdf.parse(currentDate)
            if (d1 != null && d2 != null) {
                val diffTime = d2.time - d1.time
                val diffDays = diffTime / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) {
                    // Incremented streak
                    prefs.edit()
                        .putInt("streak_count_$user", currentStreak + 1)
                        .putString("streak_last_date_$user", currentDate)
                        .apply()
                    return true
                } else if (diffDays > 1L) {
                    // Streak broken, reset to 1
                    prefs.edit()
                        .putInt("streak_count_$user", 1)
                        .putString("streak_last_date_$user", currentDate)
                        .apply()
                    return true
                }
            }
        } catch (e: Exception) {
            // Parsing error, fallback reset
            prefs.edit()
                .putInt("streak_count_$user", 1)
                .putString("streak_last_date_$user", currentDate)
                .apply()
            return true
        }
        return false
    }

    /**
     * Set reminder enabled or disabled for current user.
     */
    fun setReminderEnabled(enabled: Boolean) {
        val user = getCurrentUser() ?: "guest"
        prefs.edit().putBoolean("reminder_enabled_$user", enabled).apply()
    }

    /**
     * Check if reminder is enabled for current user.
     */
    fun isReminderEnabled(): Boolean {
        val user = getCurrentUser() ?: "guest"
        return prefs.getBoolean("reminder_enabled_$user", false)
    }

    /**
     * Save reminder time for current user.
     */
    fun setReminderTime(hour: Int, minute: Int) {
        val user = getCurrentUser() ?: "guest"
        prefs.edit()
            .putInt("reminder_hour_$user", hour)
            .putInt("reminder_minute_$user", minute)
            .apply()
    }

    /**
     * Get reminder hour for current user.
     */
    fun getReminderHour(): Int {
        val user = getCurrentUser() ?: "guest"
        return prefs.getInt("reminder_hour_$user", 20) // default 8 PM
    }

    /**
     * Get reminder minute for current user.
     */
    fun getReminderMinute(): Int {
        val user = getCurrentUser() ?: "guest"
        return prefs.getInt("reminder_minute_$user", 0) // default 00
    }

    /**
     * Save reminder days of the week for current user.
     * Days are stored as a comma-separated string of Calendar day-of-week integers.
     */
    fun setReminderDays(days: Set<Int>) {
        val user = getCurrentUser() ?: "guest"
        val daysStr = days.joinToString(",")
        prefs.edit().putString("reminder_days_$user", daysStr).apply()
    }

    /**
     * Get reminder days of the week for current user.
     * Defaults to all days of the week (1 to 7).
     */
    fun getReminderDays(): Set<Int> {
        val user = getCurrentUser() ?: "guest"
        val daysStr = prefs.getString("reminder_days_$user", "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        if (daysStr.isEmpty()) return emptySet()
        return daysStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
