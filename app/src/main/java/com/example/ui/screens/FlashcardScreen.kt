package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.QuizWithQuestions
import com.example.ui.QuizViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    viewModel: QuizViewModel,
    quizId: Long,
    onNavigateBack: () -> Unit
) {
    val allQuizzes by viewModel.quizzes.collectAsState(initial = emptyList())
    val quizWithQuestions = allQuizzes.find { it.quiz.id == quizId }

    if (quizWithQuestions == null || quizWithQuestions.questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thẻ ghi nhớ") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy câu hỏi để hiển thị Flashcard.")
            }
        }
        return
    }

    val questions = quizWithQuestions.questions
    val totalQuestions = questions.size

    // Study index pointer states
    var shuffledIndices by remember(questions) {
        mutableStateOf(questions.indices.toList().shuffled())
    }
    var currentPointer by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isSessionFinished by remember { mutableStateOf(false) }

    // Memorized / Unmemorized tracking lists
    val knownIds = remember { mutableStateListOf<Long>() }
    val unknownIds = remember { mutableStateListOf<Long>() }

    // Gesture control animators
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Reset card side and position offset when card pointer index shifts
    LaunchedEffect(currentPointer, isSessionFinished) {
        isFlipped = false
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    LaunchedEffect(isSessionFinished) {
        if (isSessionFinished) {
            viewModel.updateStreak()
        }
    }

    val currentQuestionIndex = shuffledIndices.getOrNull(currentPointer) ?: 0
    val currentQuestion = questions.getOrNull(currentQuestionIndex) ?: return

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "CardFlipAnimation"
    )

    fun handleCardOutcome(questionId: Long, known: Boolean) {
        if (known) {
            knownIds.add(questionId)
        } else {
            unknownIds.add(questionId)
        }

        // Delay to allow slide-out animation to show before loading next card
        scope.launch {
            kotlinx.coroutines.delay(100)
            if (currentPointer < shuffledIndices.size - 1) {
                currentPointer++
            } else {
                isSessionFinished = true
                viewModel.updateStreak()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Học Flashcard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = quizWithQuestions.quiz.title,
                            fontSize = 11.sp,
                            color = SecondaryTextColor,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (!isSessionFinished) {
                        IconButton(
                            onClick = {
                                shuffledIndices = shuffledIndices.shuffled()
                                currentPointer = 0
                                knownIds.clear()
                                unknownIds.clear()
                                isFlipped = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Trộn câu hỏi",
                                tint = PrimaryPurple
                            )
                        }
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        if (isSessionFinished) {
            // MÀN HÌNH KẾT QUẢ VỚI ĐỒ THỊ HÌNH TRÒN
            val totalReviewed = shuffledIndices.size
            val knownCount = knownIds.size
            val unknownCount = unknownIds.size
            val percentKnown = if (totalReviewed > 0) (knownCount * 100) / totalReviewed else 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HighDensityBackground)
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    text = "Hoàn Thành Phiên Học!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = PrimaryPurple,
                    textAlign = TextAlign.Center
                )

                // Biểu đồ hình tròn tự vẽ bằng Canvas
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidthPx = 36f
                        val radius = (size.minDimension - strokeWidthPx) / 2
                        
                        // Vòng xám nền đại diện cho tổng số
                        drawCircle(
                            color = Color(0xFFECEFF1),
                            radius = radius,
                            style = Stroke(width = strokeWidthPx)
                        )
                        
                        val sweepAngle = (percentKnown.toFloat() / 100f) * 360f
                        
                        // Cung tròn xanh đại diện cho số thẻ "Đã thuộc"
                        drawArc(
                            color = CorrectGreen,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )

                        // Cung tròn đỏ đại diện cho số thẻ "Chưa thuộc"
                        if (percentKnown < 100) {
                            drawArc(
                                color = ErrorRed,
                                startAngle = -90f + sweepAngle,
                                sweepAngle = 360f - sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${percentKnown}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryPurple
                        )
                        Text(
                            text = "Đã thuộc",
                            fontSize = 12.sp,
                            color = SecondaryTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Chi tiết thống kê
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(CorrectGreen))
                                Text("Đã thuộc bài", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("$knownCount câu hỏi", fontWeight = FontWeight.Bold, color = CorrectGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ErrorRed))
                                Text("Cần ôn tập thêm", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("$unknownCount câu hỏi", fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }
                }

                // Nút hành động ôn tập tiếp hoặc ôn tập lại
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (unknownCount > 0) {
                        Button(
                            onClick = {
                                // Chỉ ôn tiếp những câu hỏi trong danh sách chưa thuộc
                                val unlearnedIds = unknownIds.toList()
                                shuffledIndices = questions.mapIndexedNotNull { index, question ->
                                    if (unlearnedIds.contains(question.id)) index else null
                                }.shuffled()
                                
                                knownIds.clear()
                                unknownIds.clear()
                                currentPointer = 0
                                isSessionFinished = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ôn tiếp các câu chưa thuộc (${unknownCount})", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            // Reset toàn bộ phiên ôn tập lại từ đầu cho toàn bộ thẻ
                            shuffledIndices = questions.indices.toList().shuffled()
                            knownIds.clear()
                            unknownIds.clear()
                            currentPointer = 0
                            isSessionFinished = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, PrimaryPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ôn tập lại từ đầu", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quay về Trang chủ")
                    }
                }
            }
        } else {
            // MÀN HÌNH HỌC FLASHCARD VỚI CỬ CHỈ VUỐT KÉO
            val tiltRotation = offsetX.value / 25f
            val progressFraction = (currentPointer + 1).toFloat() / shuffledIndices.size.toFloat()

            // Swiping label alpha calculations
            val alphaRight = (offsetX.value / 200f).coerceIn(0f, 1f)
            val alphaLeft = (-offsetX.value / 200f).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HighDensityBackground)
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Card Progress Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Thẻ ${currentPointer + 1} trên ${shuffledIndices.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SecondaryTextColor
                    )
                    
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = PrimaryPurple,
                        trackColor = BorderColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Flippable & Swipable 3D Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                            .graphicsLayer {
                                rotationY = rotation
                                rotationZ = tiltRotation
                                cameraDistance = 14f * density
                            }
                            .clickable { isFlipped = !isFlipped }
                            .pointerInput(currentPointer) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo(offsetY.value + dragAmount.y)
                                        }
                                    },
                                    onDragEnd = {
                                        val swipeThreshold = 350f
                                        scope.launch {
                                            if (offsetX.value > swipeThreshold) {
                                                // Swipe Right -> Learned (Đã thuộc)
                                                offsetX.animateTo(700f, tween(150))
                                                handleCardOutcome(currentQuestion.id, true)
                                            } else if (offsetX.value < -swipeThreshold) {
                                                // Swipe Left -> Unlearned (Chưa thuộc)
                                                offsetX.animateTo(-700f, tween(150))
                                                handleCardOutcome(currentQuestion.id, false)
                                            } else {
                                                // Spring back to center
                                                launch { offsetX.animateTo(0f, spring()) }
                                                launch { offsetY.animateTo(0f, spring()) }
                                            }
                                        }
                                    }
                                )
                            },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (rotation <= 90f) {
                                // FRONT SIDE (Question Layout)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LightPurpleContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "CÂU HỎI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkPurple,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Text(
                                            text = currentQuestion.text,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = BodyTextColor,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 28.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(20.dp))
                                        
                                        Text(
                                            text = "Chạm để lật thẻ 💡",
                                            fontSize = 12.sp,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                // BACK SIDE (Correct Option + AI Explanation)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationY = 180f // Reverse rotation back to normal readability
                                        }
                                        .padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "ĐÁP ÁN ĐÚNG",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CorrectGreen,
                                                letterSpacing = 1.sp
                                            )

                                            val correctOption = currentQuestion.options.getOrNull(currentQuestion.correctOptionIndex) ?: ""
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CorrectGreen.copy(alpha = 0.08f))
                                                    .border(1.dp, CorrectGreen, RoundedCornerShape(14.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = CorrectGreen)
                                                    Text(
                                                        text = correctOption,
                                                        color = CorrectGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // AI Explanation section
                                            val explanations by viewModel.explanations.collectAsState()
                                            val loadingExplanations by viewModel.loadingExplanations.collectAsState()
                                            val explanation = explanations[currentQuestion.id]
                                            val isLoading = loadingExplanations.contains(currentQuestion.id)

                                            if (explanation != null) {
                                                Text(
                                                    text = "GIẢI THÍCH CỦA AI SƯ PHỤ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple,
                                                    letterSpacing = 1.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(LightPurpleContainer.copy(alpha = 0.3f))
                                                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                        Text(
                                                            text = explanation,
                                                            fontSize = 12.sp,
                                                            color = BodyTextColor,
                                                            lineHeight = 18.sp
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isLoading) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(24.dp))
                                                            Text("AI Sư phụ đang viết giải nghĩa...", fontSize = 11.sp, color = SecondaryTextColor)
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                viewModel.getAIExplanation(currentQuestion, currentQuestion.correctOptionIndex)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = LightPurpleContainer)
                                                        ) {
                                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkPurple, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Hỏi AI giải thích câu này", color = DarkPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Chạm để quay lại câu hỏi 💡",
                                            fontSize = 11.sp,
                                            color = SecondaryTextColor,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }

                        }
                    }

                    // Swipe Left / Right Label Overlays (Tinder-style) - Rendered outside Card to prevent rounded corner clipping
                    if (alphaRight > 0f || alphaLeft > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(370.dp) // Same height as Card
                                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                .graphicsLayer {
                                    rotationZ = tiltRotation
                                }
                        ) {
                            if (alphaRight > 0f) {
                                // Green overlay tint matching card shape
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(CorrectGreen.copy(alpha = alphaRight * 0.15f))
                                )
                                
                                // "ĐÃ THUỘC" stamp
                                Text(
                                    text = "ĐÃ THUỘC",
                                    color = CorrectGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 20.dp, start = 12.dp)
                                        .graphicsLayer { 
                                            rotationZ = -15f
                                            clip = false
                                            alpha = alphaRight * 0.5f // Translucent watermark style
                                        }
                                        .padding(20.dp) // Safety margin to prevent rotated layout edge clipping
                                        .border(2.dp, CorrectGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            if (alphaLeft > 0f) {
                                // Red overlay tint matching card shape
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(ErrorRed.copy(alpha = alphaLeft * 0.15f))
                                )
                                
                                // "CHƯA THUỘC" stamp
                                Text(
                                    text = "CHƯA THUỘC",
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 20.dp, end = 12.dp)
                                        .graphicsLayer { 
                                            rotationZ = 15f
                                            clip = false
                                            alpha = alphaLeft * 0.5f // Translucent watermark style
                                        }
                                        .padding(20.dp) // Safety margin to prevent rotated layout edge clipping
                                        .border(2.dp, ErrorRed, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Swiping Button Row Controls (❌ / ✔)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button Left: Still Learning (Red ❌)
                    IconButton(
                        onClick = {
                            scope.launch {
                                offsetX.animateTo(-700f, tween(180))
                                handleCardOutcome(currentQuestion.id, false)
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.1f))
                            .border(2.dp, ErrorRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chưa thuộc",
                            tint = ErrorRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Button Right: Learned (Green ✔)
                    IconButton(
                        onClick = {
                            scope.launch {
                                offsetX.animateTo(700f, tween(180))
                                handleCardOutcome(currentQuestion.id, true)
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(CorrectGreen.copy(alpha = 0.1f))
                            .border(2.dp, CorrectGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Đã thuộc",
                            tint = CorrectGreen,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}
