package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.QuizViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuizPlayScreen
import com.example.ui.screens.QuizReviewScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: QuizViewModel by viewModels {
        val app = application as QuizApplication
        QuizViewModel.provideFactory(
            quizRepository = app.container.quizRepository,
            aiRepository = app.container.aiRepository,
            userManager = app.container.userManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                var currentScreen by remember { mutableStateOf("home") }
                var selectedQuizId by remember { mutableLongStateOf(0L) }
                var selectedSessionId by remember { mutableLongStateOf(0L) }

                if (currentUser == null) {
                    AuthScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = "home"
                        }
                    )
                } else {
                    when (currentScreen) {
                        "home" -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onStartQuiz = { quizWithQuestions ->
                                    viewModel.startQuizSession(quizWithQuestions)
                                    currentScreen = "play"
                                },
                                onViewSession = { quizWithQuestions, sessionId ->
                                    selectedQuizId = quizWithQuestions.quiz.id
                                    selectedSessionId = sessionId
                                    currentScreen = "review"
                                },
                                onNavigateToCamera = {
                                    currentScreen = "camera"
                                }
                            )
                        }
                        "camera" -> {
                            CameraScanScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "home" },
                                onNavigateToHome = { currentScreen = "home" }
                            )
                        }
                        "play" -> {
                            QuizPlayScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "home" },
                                onViewSession = { sessionId ->
                                    val activeQuiz = viewModel.activeQuiz.value
                                    selectedQuizId = activeQuiz.quizWithQuestions?.quiz?.id ?: 0L
                                    selectedSessionId = sessionId
                                    currentScreen = "review"
                                }
                            )
                        }
                        "review" -> {
                            QuizReviewScreen(
                                viewModel = viewModel,
                                quizId = selectedQuizId,
                                sessionId = selectedSessionId,
                                onNavigateHome = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}
