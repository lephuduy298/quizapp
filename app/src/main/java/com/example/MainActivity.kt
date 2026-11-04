package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AdaptivePracticeViewModel
import com.example.ui.ReviewViewModel
import com.example.ui.screens.SpacedReviewScreen
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.AdaptiveLearningScreen
import com.example.data.repository.LearningRepository
import androidx.compose.ui.Modifier
import com.example.ui.QuizViewModel
import com.example.ui.QuizMode
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ManageQuestionsScreen
import com.example.ui.screens.QuizPlayScreen
import com.example.ui.screens.QuizReviewScreen
import com.example.ui.screens.FlashcardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_WIDGET_REVIEW = "com.example.action.WIDGET_REVIEW"
        const val ACTION_REVIEW_REMINDER = "com.example.action.REVIEW_REMINDER"
    }

    private var widgetRequest by mutableStateOf(0)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("widget_request", widgetRequest)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isReviewRequest(intent)) widgetRequest++
    }

    override fun onResume() {
        super.onResume()
        com.example.widget.StudyWidgetUpdates.requestRefresh()
        com.example.reminder.ReviewDueReminder.refresh(this)
    }

    private val viewModel: QuizViewModel by viewModels {
        val app = application as QuizApplication
        QuizViewModel.provideFactory(
            quizRepository = app.container.quizRepository,
            aiRepository = app.container.aiRepository,
            userManager = app.container.userManager,
            reviewRepository = app.container.reviewRepository
        )
    }

    private fun isReviewRequest(intent: Intent) =
        intent.action == ACTION_WIDGET_REVIEW || intent.action == ACTION_REVIEW_REMINDER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetRequest = savedInstanceState?.getInt("widget_request") ?: 0
        if (savedInstanceState == null && isReviewRequest(intent)) widgetRequest++
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                var currentScreen by rememberSaveable { mutableStateOf("home") }
                var reviewInitialTab by rememberSaveable { mutableIntStateOf(0) }
                var pendingWidgetReview by rememberSaveable { mutableStateOf(false) }
                var handledWidgetRequest by rememberSaveable { mutableIntStateOf(0) }
                LaunchedEffect(widgetRequest, currentUser) {
                    if (widgetRequest > handledWidgetRequest) {
                        pendingWidgetReview = true
                        handledWidgetRequest = widgetRequest
                    }
                    if (pendingWidgetReview && currentUser != null) {
                        reviewInitialTab = 0
                        currentScreen = "spaced_review"
                        pendingWidgetReview = false
                    }
                }
                var selectedQuizId by rememberSaveable { mutableLongStateOf(0L) }
                var selectedSessionId by rememberSaveable { mutableLongStateOf(0L) }
                var selectedFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
                LaunchedEffect(currentScreen) {
                    // The legacy exam state is in memory; only the adaptive session is persisted.
                    if (currentScreen == "play" && viewModel.activeQuiz.value.quizWithQuestions == null) {
                        currentScreen = "home"
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = if (pendingWidgetReview) "spaced_review" else "home"
                        }
                    )
                } else {
                    when (currentScreen) {
                        "home" -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onStudy = { currentScreen = "study_hub" },
                                onStartQuiz = { quizWithQuestions, mode ->
                                    viewModel.startQuizSession(quizWithQuestions, mode)
                                    currentScreen = "play"
                                },
                                onStartFlashcards = { quizWithQuestions ->
                                    selectedQuizId = quizWithQuestions.quiz.id
                                    currentScreen = "flashcard"
                                },
                                onEditQuiz = { quizWithQuestions ->
                                    selectedQuizId = quizWithQuestions.quiz.id
                                    currentScreen = "manage_questions"
                                },
                                onViewSession = { quizWithQuestions, sessionId ->
                                    selectedQuizId = quizWithQuestions.quiz.id
                                    selectedSessionId = sessionId
                                    currentScreen = "review"
                                },
                                onNavigateToCamera = { folderId ->
                                    selectedQuizId = 0L // Creating new quiz
                                    selectedFolderId = folderId
                                    currentScreen = "camera"
                                }
                            )
                        }
                        "camera" -> {
                            CameraScanScreen(
                                viewModel = viewModel,
                                targetQuizId = if (selectedQuizId == 0L) null else selectedQuizId,
                                targetFolderId = selectedFolderId,
                                onNavigateBack = { 
                                    if (selectedQuizId == 0L) currentScreen = "home" 
                                    else currentScreen = "manage_questions" 
                                },
                                onNavigateToHome = { currentScreen = "home" }
                            )
                        }
                        "manage_questions" -> {
                            ManageQuestionsScreen(
                                viewModel = viewModel,
                                quizId = selectedQuizId,
                                onAdaptiveLearning = { currentScreen = "adaptive" },
                                onNavigateBack = { currentScreen = "home" },
                                onAddViaAI = { quizId ->
                                    selectedQuizId = quizId
                                    currentScreen = "camera"
                                }
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
                        "study_hub" -> {
                            val userKey = LearningRepository.userKey(requireNotNull(currentUser))
                            val reviewViewModel: ReviewViewModel = viewModel(key = "review:$userKey",
                                factory = ReviewViewModel.factory((application as QuizApplication).container.reviewRepository, userKey))
                            StudyHubScreen(reviewViewModel, onBack = { currentScreen = "home" },
                                onPractice = { selectedQuizId = 0L; currentScreen = "adaptive" },
                                onDueReview = { reviewInitialTab = 0; currentScreen = "spaced_review" },
                                onMistakes = { reviewInitialTab = 1; currentScreen = "spaced_review" })
                        }
                        "spaced_review" -> {
                            val userKey = LearningRepository.userKey(requireNotNull(currentUser))
                            val reviewViewModel: ReviewViewModel = viewModel(key = "review:$userKey",
                                factory = ReviewViewModel.factory((application as QuizApplication).container.reviewRepository, userKey))
                            key(userKey, reviewInitialTab, handledWidgetRequest) {
                                SpacedReviewScreen(reviewViewModel, onBack = { currentScreen = "study_hub" }, initialTab = reviewInitialTab,
                                    onStudied = {
                                        if (LearningRepository.userKey(viewModel.currentUser.value ?: "") == userKey) viewModel.updateStreak()
                                    })
                            }
                        }
                        "adaptive" -> {
                            val userKey = LearningRepository.userKey(requireNotNull(currentUser))
                            val adaptiveViewModel: AdaptivePracticeViewModel = viewModel(
                                key = "adaptive:$userKey",
                                factory = AdaptivePracticeViewModel.factory((application as QuizApplication).container.learningRepository, userKey)
                            )
                            AdaptiveLearningScreen(adaptiveViewModel, initialQuizId = selectedQuizId.takeIf { it != 0L },
                                onBack = { currentScreen = if (selectedQuizId == 0L) "study_hub" else "manage_questions" })
                        }
                        "flashcard" -> {
                            FlashcardScreen(
                                viewModel = viewModel,
                                quizId = selectedQuizId,
                                onNavigateBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}
