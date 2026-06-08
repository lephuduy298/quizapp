package com.example.ui

import android.graphics.Bitmap
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.QuizApplication
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizEntity
import com.example.data.local.QuizSessionEntity
import com.example.data.local.QuizWithQuestions
import com.example.data.local.UserManager
import com.example.data.repository.AIRepository
import com.example.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizGenerationState {
    object Idle : QuizGenerationState
    object Scanning : QuizGenerationState
    object Generating : QuizGenerationState
    object Success : QuizGenerationState
    data class Error(val message: String) : QuizGenerationState
}

data class ActiveQuizState(
    val quizWithQuestions: QuizWithQuestions? = null,
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Long, Int> = emptyMap(), // Question Id -> Selected option index
    val secondsRemaining: Int = 0,
    val isTimerRunning: Boolean = false,
    val isSubmitted: Boolean = false,
    val sessionEntity: QuizSessionEntity? = null
)

class QuizViewModel(
    private val quizRepository: QuizRepository,
    private val aiRepository: AIRepository,
    private val userManager: UserManager
) : ViewModel() {

    // User authentication state
    private val _currentUser = MutableStateFlow<String?>(userManager.getCurrentUser())
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    fun login(username: String, password: String): Boolean {
        val success = userManager.login(username, password)
        if (success) {
            _currentUser.value = userManager.getCurrentUser()
        }
        return success
    }

    fun register(username: String, password: String): Boolean {
        return userManager.register(username, password)
    }

    fun logout() {
        userManager.logout()
        _currentUser.value = null
    }

    fun isUserLoggedIn(): Boolean {
        return userManager.isUserLoggedIn()
    }

    // List of quizzes with questions
    val quizzes = quizRepository.allQuizzesWithQuestions

    // History logs of submitted sessions
    val sessions = quizRepository.allSessions

    // AI Generation state
    private val _generationState = MutableStateFlow<QuizGenerationState>(QuizGenerationState.Idle)
    val generationState: StateFlow<QuizGenerationState> = _generationState.asStateFlow()

    // Screen navigation / active test control state
    private val _activeQuiz = MutableStateFlow(ActiveQuizState())
    val activeQuiz: StateFlow<ActiveQuizState> = _activeQuiz.asStateFlow()

    // AI explanations caching
    private val _explanations = MutableStateFlow<Map<Long, String>>(emptyMap())
    val explanations: StateFlow<Map<Long, String>> = _explanations.asStateFlow()

    // Explanation loading states
    private val _loadingExplanations = MutableStateFlow<Set<Long>>(emptySet())
    val loadingExplanations: StateFlow<Set<Long>> = _loadingExplanations.asStateFlow()

    private var countdownTimer: CountDownTimer? = null

    // Reset API Generation State
    fun resetGenerationState() {
        _generationState.value = QuizGenerationState.Idle
    }

    // Capture image & request quiz generation from Gemini
    fun generateQuizFromImage(bitmap: Bitmap, promptAddition: String = "") {
        _generationState.value = QuizGenerationState.Generating
        viewModelScope.launch {
            val result = aiRepository.generateQuizFromImage(bitmap, promptAddition)
            result.fold(
                onSuccess = { generated ->
                    try {
                        // Persist quiz to Database
                        val quizEntity = QuizEntity(
                            title = generated.title,
                            topic = generated.topic,
                            durationMinutes = 10 + (generated.questions.size * 2) // Dynamic quiz length
                        )
                        val questionEntities = generated.questions.map { q ->
                            QuestionEntity(
                                quizId = 0, // Assigned inside the repository transaction
                                text = q.text,
                                options = q.options,
                                correctOptionIndex = q.correctOptionIndex
                            )
                        }

                        quizRepository.insertQuizWithQuestions(quizEntity, questionEntities)
                        _generationState.value = QuizGenerationState.Success
                    } catch (e: Exception) {
                        _generationState.value = QuizGenerationState.Error("DB error: " + (e.message ?: "Failed to save quiz."))
                    }
                },
                onFailure = { error ->
                    _generationState.value = QuizGenerationState.Error(error.message ?: "Unknown error while calling AI.")
                }
            )
        }
    }

    // Create a mock default or quick manual quiz
    fun createQuickQuiz(title: String, topic: String, duration: Int, questionsList: List<QuestionEntity>) {
        viewModelScope.launch {
            val quizEntity = QuizEntity(title = title, topic = topic, durationMinutes = duration)
            quizRepository.insertQuizWithQuestions(quizEntity, questionsList)
        }
    }

    // Delete a particular quiz
    fun deleteQuiz(quizId: Long) {
        viewModelScope.launch {
            quizRepository.deleteQuizById(quizId)
        }
    }

    // --- QUIZ GAMEPLAY CONTROL ---

    // Initialize an active quiz session
    fun startQuizSession(quizWithQuestions: QuizWithQuestions) {
        countdownTimer?.cancel()

        val totalDurationSeconds = quizWithQuestions.quiz.durationMinutes * 60
        _activeQuiz.value = ActiveQuizState(
            quizWithQuestions = quizWithQuestions,
            currentQuestionIndex = 0,
            selectedAnswers = emptyMap(),
            secondsRemaining = totalDurationSeconds,
            isTimerRunning = true,
            isSubmitted = false,
            sessionEntity = null
        )

        _explanations.value = emptyMap() // Clear old explanations
        _loadingExplanations.value = emptySet()

        startTimer(totalDurationSeconds)
    }

    private fun startTimer(seconds: Int) {
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecs = (millisUntilFinished / 1000).toInt()
                _activeQuiz.value = _activeQuiz.value.copy(
                    secondsRemaining = currentSecs
                )
            }

            override fun onFinish() {
                _activeQuiz.value = _activeQuiz.value.copy(
                    secondsRemaining = 0,
                    isTimerRunning = false
                )
                submitQuiz()
            }
        }.start()
    }

    // Select an answer for current active question
    fun selectAnswer(questionId: Long, optionIndex: Int) {
        val currentState = _activeQuiz.value
        if (currentState.isSubmitted) return

        val updatedAnswers = currentState.selectedAnswers.toMutableMap()
        updatedAnswers[questionId] = optionIndex

        _activeQuiz.value = currentState.copy(
            selectedAnswers = updatedAnswers
        )
    }

    // Move next or previous question
    fun navigateToQuestion(index: Int) {
        val currentState = _activeQuiz.value
        val quiz = currentState.quizWithQuestions ?: return
        if (index in 0 until quiz.questions.size) {
            _activeQuiz.value = currentState.copy(
                currentQuestionIndex = index
            )
        }
    }

    // Submit the quiz attempt, grade correctness & save log
    fun submitQuiz() {
        countdownTimer?.cancel()
        val currentState = _activeQuiz.value
        val quiz = currentState.quizWithQuestions ?: return
        if (currentState.isSubmitted) return

        // Calculate score
        var correctCount = 0
        quiz.questions.forEach { question ->
            val selected = currentState.selectedAnswers[question.id]
            if (selected == question.correctOptionIndex) {
                correctCount++
            }
        }

        val totalQuestions = quiz.questions.size
        val percentage = if (totalQuestions > 0) {
            (correctCount.toDouble() / totalQuestions.toDouble()) * 100.0
        } else {
            0.0
        }

        // Prepare QuizSessionEntity
        val session = QuizSessionEntity(
            quizId = quiz.quiz.id,
            startTime = System.currentTimeMillis() - ((quiz.quiz.durationMinutes * 60) - currentState.secondsRemaining) * 1000,
            endTime = System.currentTimeMillis(),
            score = percentage,
            correctAnswersCount = correctCount,
            totalQuestionsCount = totalQuestions,
            answersMap = currentState.selectedAnswers
        )

        viewModelScope.launch {
            val sessionId = quizRepository.insertSession(session)
            _activeQuiz.value = currentState.copy(
                isSubmitted = true,
                isTimerRunning = false,
                sessionEntity = session.copy(id = sessionId)
            )
        }
    }

    // --- AI TUTOR EXPLAINER ENGINE ---

    fun getAIExplanation(question: QuestionEntity, selectedIndex: Int) {
        val questionId = question.id
        if (_explanations.value.containsKey(questionId) || _loadingExplanations.value.contains(questionId)) {
            return
        }

        _loadingExplanations.value = _loadingExplanations.value + questionId

        viewModelScope.launch {
            val correctText = question.options.getOrNull(question.correctOptionIndex) ?: "N/A"
            val userText = question.options.getOrNull(selectedIndex) ?: "N/A"

            val result = aiRepository.getExplanationForAnswer(
                questionText = question.text,
                options = question.options,
                correctAnswer = correctText,
                userAnswer = userText
            )

            result.fold(
                onSuccess = { explanation ->
                    val updated = _explanations.value.toMutableMap()
                    updated[questionId] = explanation
                    _explanations.value = updated
                },
                onFailure = { error ->
                    val updated = _explanations.value.toMutableMap()
                    updated[questionId] = "Không thể tải giải tích từ Gemini trợ lý: \n${error.message}"
                    _explanations.value = updated
                }
            )
            _loadingExplanations.value = _loadingExplanations.value - questionId
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownTimer?.cancel()
    }

    // Manual Factory provider to load without Hilt (robust setup)
    companion object {
        fun provideFactory(
            quizRepository: QuizRepository,
            aiRepository: AIRepository,
            userManager: UserManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(quizRepository, aiRepository, userManager) as T
            }
        }
    }
}
