package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizReviewScreen(
    viewModel: QuizViewModel,
    quizId: Long,
    sessionId: Long,
    onNavigateHome: () -> Unit
) {
    val activeState by viewModel.activeQuiz.collectAsState()
    val explanations by viewModel.explanations.collectAsState()
    val loadingExplanations by viewModel.loadingExplanations.collectAsState()

    val activeQuiz = activeState.quizWithQuestions
    val activeSession = activeState.sessionEntity

    // Locate matching quiz & session if not already pre-populated inside activeState
    LaunchedEffect(quizId, sessionId) {
        viewModel.loadQuizAndSession(quizId, sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xem lại & Đánh giá kết quả") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Trang chủ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (activeQuiz == null || activeSession == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            val quiz = activeQuiz
            val session = activeSession

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HighDensityBackground)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assessment Score Header Card
                item {
                    val statusColor = if (session.score >= 50.0) CorrectGreen else ErrorRed
                    val statusText = if (session.score >= 80.0) "Xuất sắc!"
                    else if (session.score >= 50.0) "Đạt yêu cầu"
                    else "Cần cố gắng học thêm"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = quiz.quiz.title.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                letterSpacing = 1.2.sp
                            )

                            // Circular visual score display
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${session.score.toInt()}%",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = statusColor
                                        )
                                        Text(
                                            text = "Kết quả",
                                            fontSize = 10.sp,
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            Text(
                                text = statusText,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = statusColor
                            )

                            Text(
                                text = "Làm đúng ${session.correctAnswersCount}/${session.totalQuestionsCount} câu • Làm bài trong 15 phút",
                                fontSize = 12.sp,
                                color = SecondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Questions Review Header
                item {
                    Text(
                        text = "DANH SÁCH CHI TIẾT CÂU HỎI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTextColor,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Displaying Questions + Options + AI Tutoring button
                itemsIndexed(quiz.questions) { index, question ->
                    val selectedOption = session.answersMap[question.id]
                    val isCorrect = selectedOption == question.correctOptionIndex

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(HighDensityBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = BodyTextColor
                                        )
                                    }
                                    Text(
                                        text = "Đáp án lựa chọn",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTextColor
                                    )
                                }

                                // Status Correct/Wrong Text Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCorrect) CorrectGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isCorrect) "Chính xác" else "Trả lời sai",
                                        color = if (isCorrect) CorrectGreen else ErrorRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Question Title
                            Text(
                                text = question.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BodyTextColor,
                                lineHeight = 20.sp
                            )

                            // Option review summary
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                question.options.forEachIndexed { optIndex, optionText ->
                                    val isUserSelected = selectedOption == optIndex
                                    val isCorrectOption = question.correctOptionIndex == optIndex

                                    val itemBg = if (isCorrectOption) CorrectGreen.copy(alpha = 0.08f)
                                    else if (isUserSelected) ErrorRed.copy(alpha = 0.08f)
                                    else Color.White

                                    val borderCol = if (isCorrectOption) CorrectGreen
                                    else if (isUserSelected) ErrorRed
                                    else BorderColor.copy(alpha = 0.5f)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(itemBg)
                                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCorrectOption) Icons.Outlined.CheckCircle
                                            else if (isUserSelected) Icons.Outlined.Cancel
                                            else Icons.Outlined.Circle,
                                            contentDescription = null,
                                            tint = if (isCorrectOption) CorrectGreen
                                            else if (isUserSelected) ErrorRed
                                            else SecondaryTextColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Text(
                                            text = optionText,
                                            fontSize = 13.sp,
                                            color = if (isCorrectOption) CorrectGreen
                                            else if (isUserSelected) ErrorRed
                                            else BodyTextColor,
                                            fontWeight = if (isCorrectOption || isUserSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // AI TUMOR COALITION WORK
                            val explanation = explanations[question.id]
                            val isLoading = loadingExplanations.contains(question.id)

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
                                                    text = "Giải thích từ Sư phụ AI",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = DarkPurple
                                                )
                                            }
                                            
                                            // Show retry icon only if it's an error message
                                            if (explanation.startsWith("Không thể tải giải tích")) {
                                                IconButton(
                                                    onClick = { viewModel.getAIExplanation(question, selectedOption ?: -1) },
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
                                    onClick = { viewModel.getAIExplanation(question, selectedOption ?: -1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LightPurpleContainer),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_explain_button_${question.id}"),
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
                                            Text("Sư phụ AI đang giải nghĩa...", color = DarkPurple, fontSize = 12.sp)
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

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
