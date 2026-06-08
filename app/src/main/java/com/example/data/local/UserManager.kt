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
}
