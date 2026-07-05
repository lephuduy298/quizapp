package com.example

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UserManager
import com.example.data.remote.RetrofitClient
import com.example.data.repository.AIRepository
import com.example.data.repository.DirectGeminiAIRepositoryImpl
import com.example.data.repository.QuizRepository

interface AppContainer {
    val quizRepository: QuizRepository
    val aiRepository: AIRepository
    val userManager: UserManager
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val quizRepository: QuizRepository by lazy {
        QuizRepository(
            quizDao = database.quizDao(),
            quizSessionDao = database.quizSessionDao(),
            folderDao = database.folderDao()
        )
    }

    override val aiRepository: AIRepository by lazy {
        DirectGeminiAIRepositoryImpl(
            apiService = RetrofitClient.geminiApiService
        )
    }

    override val userManager: UserManager by lazy {
        UserManager.getInstance(context)
    }
}
