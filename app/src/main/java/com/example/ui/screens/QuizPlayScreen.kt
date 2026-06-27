package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizViewModel
import com.example.ui.QuizMode
import com.example.ui.theme.*

@Composable
fun QuizPlayScreen(
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    onViewSession: (Long) -> Unit
) {
    val activeState by viewModel.activeQuiz.collectAsState()
    val quiz = activeState.quizWithQuestions ?: return

    val currentQuestionIndex = activeState.currentQuestionIndex
    val currentQuestion = quiz.questions.getOrNull(currentQuestionIndex) ?: return
    val totalQuestions = quiz.questions.size

    val selectedAnswers = activeState.selectedAnswers
    val secondsRemaining = activeState.secondsRemaining

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    var showSubmitConfirmation by remember { mutableStateOf(false) }

    // Auto navigate to session review once submitted
    LaunchedEffect(activeState.isSubmitted, activeState.sessionEntity) {
        if (activeState.isSubmitted && activeState.sessionEntity != null) {
            onViewSession(activeState.sessionEntity!!.id)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Thoát")
                    }

                    // Progress title
                    Text(
                        text = "Câu ${currentQuestionIndex + 1}/${totalQuestions}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BodyTextColor
                    )

                    if (activeState.quizMode == QuizMode.EXAM) {
                        // Header countdown timer pill widget
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (secondsRemaining < 60) ErrorRed.copy(alpha = 0.1f) else LightPurpleContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (secondsRemaining < 60) ErrorRed else DarkPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formattedTime,
                                    color = if (secondsRemaining < 60) ErrorRed else DarkPurple,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        // Practice Mode Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightPurpleContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Luyện tập 📖",
                                color = DarkPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Header status progress bar
                val progressFraction = (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()
                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryPurple,
                    trackColor = BorderColor.copy(alpha = 0.3f)
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Question Button
                    TextButton(
                        onClick = { viewModel.navigateToQuestion(currentQuestionIndex - 1) },
                        enabled = currentQuestionIndex > 0
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null)
                        Text("Quay Lại")
                    }

                    // Submit Quiz Button
                    Button(
                        onClick = { showSubmitConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                    ) {
                        Text("Nộp Bài", fontWeight = FontWeight.Bold)
                    }

                    // NEXT Button
                    TextButton(
                        onClick = { viewModel.navigateToQuestion(currentQuestionIndex + 1) },
                        enabled = currentQuestionIndex < totalQuestions - 1
                    ) {
                        Text("Tiếp")
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HighDensityBackground)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question text box details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = quiz.quiz.title,
                        color = PrimaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentQuestion.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = BodyTextColor,
                        lineHeight = 24.sp
                    )
                }
            }

            val explanations by viewModel.explanations.collectAsState()
            val loadingExplanations by viewModel.loadingExplanations.collectAsState()
            val userSelection = selectedAnswers[currentQuestion.id]
            val correctOptionIndex = currentQuestion.correctOptionIndex
            val isPracticeMode = activeState.quizMode == QuizMode.PRACTICE

            // Options details selection lists
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(currentQuestion.options) { index, option ->
                    val isSelected = userSelection == index
                    val isCorrectOption = correctOptionIndex == index
                    val letterPrefix = when (index) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> (index + 65).toChar().toString()
                    }

                    val itemBg = when {
                        !isPracticeMode || userSelection == null -> {
                            if (isSelected) LightPurpleContainer else Color.White
                        }
                        isCorrectOption -> CorrectGreen.copy(alpha = 0.08f)
                        isSelected -> ErrorRed.copy(alpha = 0.08f)
                        else -> Color.White
                    }

                    val borderCol = when {
                        !isPracticeMode || userSelection == null -> {
                            if (isSelected) PrimaryPurple else BorderColor
                        }
                        isCorrectOption -> CorrectGreen
                        isSelected -> ErrorRed
                        else -> BorderColor.copy(alpha = 0.5f)
                    }

                    val borderWidth = when {
                        !isPracticeMode || userSelection == null -> {
                            if (isSelected) 2.dp else 1.dp
                        }
                        isCorrectOption || isSelected -> 2.dp
                        else -> 1.dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBg)
                            .border(width = borderWidth, color = borderCol, shape = RoundedCornerShape(14.dp))
                            .clickable(enabled = userSelection == null) {
                                viewModel.selectAnswer(currentQuestion.id, index)
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val circleBg = when {
                                !isPracticeMode || userSelection == null -> {
                                    if (isSelected) PrimaryPurple else HighDensityBackground
                                }
                                isCorrectOption -> CorrectGreen
                                isSelected -> ErrorRed
                                else -> HighDensityBackground
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(circleBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letterPrefix,
                                    color = if (isSelected || (isPracticeMode && userSelection != null && isCorrectOption)) Color.White else SecondaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = option,
                                color = when {
                                    !isPracticeMode || userSelection == null -> {
                                        if (isSelected) DarkPurple else BodyTextColor
                                    }
                                    isCorrectOption -> CorrectGreen
                                    isSelected -> ErrorRed
                                    else -> BodyTextColor
                                },
                                fontWeight = if (isSelected || (isPracticeMode && userSelection != null && isCorrectOption)) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (!isPracticeMode || userSelection == null) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Đã lựa chọn",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                if (isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Đáp án đúng",
                                        tint = CorrectGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đáp án bạn chọn chưa đúng",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // If user selection has been committed in Practice Mode, display AI Tutor box!
                if (isPracticeMode && userSelection != null) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isCorrect = userSelection == correctOptionIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCorrect) CorrectGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isCorrect) "🎉 Tuyệt vời! Bạn đã trả lời chính xác." else "❌ Tiếc quá! Bạn chọn chưa đúng. Đọc giải thích bên dưới nhé.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCorrect) CorrectGreen else ErrorRed
                            )
                        }
                    }

                    item {
                        val explanation = explanations[currentQuestion.id]
                        val isLoading = loadingExplanations.contains(currentQuestion.id)

                        if (explanation != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LightPurpleContainer.copy(alpha = 0.4f))
                                    .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = DarkPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Giải Thích Từ AI Trợ Lý",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = DarkPurple
                                            )
                                        }
                                        if (explanation.startsWith("Không thể tải giải tích")) {
                                            IconButton(
                                                onClick = { viewModel.getAIExplanation(currentQuestion, userSelection) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Thử lại",
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = explanation,
                                        fontSize = 12.sp,
                                        color = if (explanation.startsWith("Không thể tải giải tích")) ErrorRed else BodyTextColor,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.getAIExplanation(currentQuestion, userSelection) },
                                colors = ButtonDefaults.buttonColors(containerColor = LightPurpleContainer),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = DarkPurple,
                                            strokeWidth = 2.dp
                                        )
                                        Text("Gemini đang giải nghĩa...", color = DarkPurple, fontSize = 12.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = DarkPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Hỏi AI Sư Phụ giải thích", color = DarkPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Submit confirmation dialog overlay
    if (showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            title = { Text("Bạn có chắc muốn nộp bài?") },
            text = { Text("Bài nộp của bạn sẽ được đánh giá điểm ngay lập tức và tiến hành lưu lại trên thông tin sổ học tập.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        viewModel.submitQuiz()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                ) {
                    Text("Nộp Bài")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Làm Tiếp")
                }
            }
        )
    }
}
