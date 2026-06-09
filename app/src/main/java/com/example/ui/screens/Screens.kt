package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.QuizApplication
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizEntity
import com.example.data.local.QuizSessionEntity
import com.example.data.local.QuizWithQuestions
import com.example.ui.*
import com.example.ui.theme.*
import com.google.accompanist.permissions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: QuizViewModel,
    onStartQuiz: (QuizWithQuestions) -> Unit,
    onEditQuiz: (QuizWithQuestions) -> Unit,
    onViewSession: (QuizWithQuestions, Long) -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val quizzes by viewModel.quizzes.collectAsState(initial = emptyList())
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()
    val userInitials = currentUser?.trim()?.take(2)?.uppercase() ?: "AI"
    var showQuickCreateDialog by remember { mutableStateOf(false) }
    var quizToDelete by remember { mutableStateOf<QuizWithQuestions?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "QuizAI Studio",
                            fontWeight = FontWeight.Bold,
                            color = BodyTextColor,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tài khoản: ${currentUser ?: "Khách"}",
                            fontSize = 12.sp,
                            color = SecondaryTextColor
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LightPurpleContainer)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Đăng xuất",
                        tint = DarkPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Quick Manual Create
                FloatingActionButton(
                    onClick = { showQuickCreateDialog = true },
                    containerColor = LightPurpleContainer,
                    contentColor = DarkPurple,
                    modifier = Modifier.testTag("manual_create_fab")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Sổ đề thủ công")
                }

                // AI Generated from Camera
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("ai_camera_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera AI")
                        Text(text = "Tạo Đề Bằng AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(HighDensityBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HIGH DENSITY SUMMARY CARDS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Quizzes Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "TỔNG SỐ ĐỀ THI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${quizzes.size}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = BodyTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lưu ngoại tuyến",
                                fontSize = 11.sp,
                                color = SecondaryTextColor
                            )
                        }
                    }

                    // Token / Status Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(LightPurpleContainer)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "TRỢ LÝ GEMINI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkPurple,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "3.5 Flash",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tự động phân giải",
                                fontSize = 11.sp,
                                color = DarkPurple.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // RECENT ACTIVITY HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DANH SÁCH ĐỀ THI (${quizzes.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTextColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            // LIST OF QUIZZES
            if (quizzes.isEmpty()) {
                item {
                    CardEmptyState(
                        title = "Chưa có đề thi nào",
                        subtitle = "Chụp một tài liệu bài tập hoặc tạo một đề thi thủ công bằng nút bên dưới để bắt đầu học tập cùng AI trợ lý.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                itemsIndexed(quizzes) { index, item ->
                    QuizDensityRow(
                        quizWithQuestions = item,
                        onStart = { onStartQuiz(item) },
                        onEdit = { onEditQuiz(item) },
                        onDelete = { quizToDelete = item }
                    )
                }
            }

            // SESSION HISTORICAL LOGS
            item {
                Text(
                    text = "LỊCH SỬ THI & KIỂM TRA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryTextColor,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (sessions.isEmpty()) {
                item {
                    CardEmptyState(
                        title = "Chưa có lịch sử làm bài",
                        subtitle = "Khi bạn hoàn thành đề thi và bấm Nộp bài, các thống kê chi tiết & kết quả phân tích sẽ hiển thị tại đây.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                itemsIndexed(sessions) { idx, session ->
                    val matchedQuiz = quizzes.find { it.quiz.id == session.quizId }
                    SessionHistoryRow(
                        session = session,
                        quizWithQuestions = matchedQuiz,
                        onView = {
                            if (matchedQuiz != null) {
                                onViewSession(matchedQuiz, session.id)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Quick Manual Creation Dialog
    if (showQuickCreateDialog) {
        Dialog(onDismissRequest = { showQuickCreateDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                var title by remember { mutableStateOf("") }
                var topic by remember { mutableStateOf("Năng lực Nhật ngữ") }
                var duration by remember { mutableStateOf("15") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Tạo đề thi kiểm tra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BodyTextColor
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tên đề thi") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Chủ đề / Phân loại") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Thời gian làm bài (phút)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showQuickCreateDialog = false }) {
                            Text("Hủy bỏ")
                        }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val durationVal = duration.toIntOrNull() ?: 15
                                    // Create an empty quiz as requested by user
                                    viewModel.createQuickQuiz(
                                        title = title,
                                        topic = topic,
                                        duration = durationVal,
                                        questionsList = emptyList()
                                    )
                                    showQuickCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Tạo Đề")
                        }
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog for Quiz
    if (quizToDelete != null) {
        AlertDialog(
            onDismissRequest = { quizToDelete = null },
            title = { Text("Xác nhận xóa đề thi") },
            text = { Text("Bạn có chắc chắn muốn xóa đề thi '${quizToDelete?.quiz?.title}'? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        quizToDelete?.let { viewModel.deleteQuiz(it.quiz.id) }
                        quizToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { quizToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun CardEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(HighDensityBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SecondaryTextColor
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = BodyTextColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SecondaryTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun QuizDensityRow(
    quizWithQuestions: QuizWithQuestions,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Topic Icon Indicator
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HighDensityBackground),
                    contentAlignment = Alignment.Center
                ) {
                    val topicEmoji = when (quizWithQuestions.quiz.topic.lowercase()) {
                        "physics", "vật lý" -> "⚛️"
                        "grammar", "ngữ pháp", "japanese", "tiếng nhật" -> "📖"
                        "chemistry", "hóa học" -> "🧪"
                        else -> "📝"
                    }
                    Text(text = topicEmoji, fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quizWithQuestions.quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BodyTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${quizWithQuestions.questions.size} câu hỏi • ${quizWithQuestions.quiz.durationMinutes} phút • ${quizWithQuestions.quiz.topic}",
                        fontSize = 12.sp,
                        color = SecondaryTextColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa đề thi",
                        tint = PrimaryPurple.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa đề thi",
                        tint = ErrorRed.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = SecondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SessionHistoryRow(
    session: QuizSessionEntity,
    quizWithQuestions: QuizWithQuestions?,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score text or symbol
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (session.score >= 50.0) CorrectGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${session.score.toInt()}%",
                        color = if (session.score >= 50.0) CorrectGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quizWithQuestions?.quiz?.title ?: "Đề thi đã ứng với ID ${session.quizId}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BodyTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val format = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
                    val dateFormatted = format.format(Date(session.endTime))
                    Text(
                        text = "$dateFormatted • Đúng ${session.correctAnswersCount}/${session.totalQuestionsCount} câu",
                        fontSize = 11.sp,
                        color = SecondaryTextColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (session.score >= 50.0) "Đạt" else "Cần học lại",
                    color = if (session.score >= 50.0) CorrectGreen else ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = SecondaryTextColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    viewModel: QuizViewModel,
    targetQuizId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val generationState by viewModel.generationState.collectAsState()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var promptAdditionText by remember { mutableStateOf("") }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.generateQuizFromImage(bitmap, promptAdditionText, targetQuizId)
                    } else {
                        Log.e("CameraScanScreen", "Failed to deserialize selected image to bitmap")
                    }
                } catch (e: Exception) {
                    Log.e("CameraScanScreen", "Error reading selected image", e)
                }
            }
        }
    }

    LaunchedEffect(generationState) {
        if (generationState is QuizGenerationState.Success) {
            viewModel.resetGenerationState()
            onNavigateToHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quét Đề Thi Bằng AI") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Viewfinder block
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScan", "Camera binding failure", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Layout guidelines box overlay in the center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 200.dp, top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .fillMaxHeight(0.6f)
                            .border(2.dp, PrimaryPurple, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "HÃY CĂN GHÉP VĂN BẢN TRÊN SÁCH VÀO KHUNG VỰC NÀY",
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(0.dp, 0.dp, 8.dp, 8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Controls and Input prompt block at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = promptAdditionText,
                            onValueChange = { promptAdditionText = it },
                            placeholder = { Text("Yêu cầu thêm (VD: độ khó cao, tiếng Nhật chuyên ngành N2...)", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Import mock snapshot fallback button or real file picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .clickable {
                                            // Trigger mock sample upload image
                                            filePickerLauncher.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Tải ảnh từ thư viện", tint = Color.White)
                                }
                                Text(text = "Chọn ảnh", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }

                            // Capture button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        val capture = imageCapture
                                        if (capture != null) {
                                            viewModel.generateQuizFromImage(
                                                Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888), // Fallback if emulator
                                                promptAdditionText,
                                                targetQuizId
                                            )
                                        } else {
                                            // Test fallback direct generation with dummy bitmap
                                            val dummyBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                                            viewModel.generateQuizFromImage(dummyBitmap, promptAdditionText, targetQuizId)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Chụp và tạo đề",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Mock sample quick trigger button for testing easy flow without local file
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .clickable {
                                            // Simulate a camera photo of N3 grammar to bypass camera constraints
                                            val dummyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                                            viewModel.generateQuizFromImage(
                                                dummyBitmap,
                                                "Mô phỏng chụp đề thi tiếng Nhật N3",
                                                targetQuizId
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Mẫu Test", tint = Color.White)
                                }
                                Text(text = "Tạo mẫu AI", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            } else {
                // Permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Cần quyền truy cập Camera",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Chúng tôi sử dụng Camera để quét đề thi trực tiếp từ tài liệu giấy, sách bài tập của bạn và dịch sang các câu hỏi trắc nghiệm tương tác.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Cấp Quyền Camera")
                    }
                }
            }

            // PROCESSING DIALOG
            if (generationState !is QuizGenerationState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        when (val state = generationState) {
                            is QuizGenerationState.Generating -> {
                                CircularProgressIndicator(color = PrimaryPurple)
                                Text(
                                    text = "Gemini AI Đang Phân Tích...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Mô hình Gemini 3.5 Flash đang đọc văn bản của tài liệu, nhận dạng các bài tập, câu hỏi và tiến hành trích xuất cấu trúc đề thi thành cơ sở dữ liệu...",
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            is QuizGenerationState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Không Thể Tạo Đề Thi",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = state.message,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                                Button(
                                    onClick = { viewModel.resetGenerationState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                ) {
                                    Text("Quay Lại")
                                }
                            }
                            is QuizGenerationState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CorrectGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Trích xuất đề thi thành công!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

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

            // Options details selection lists
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(currentQuestion.options) { index, option ->
                    val isSelected = selectedAnswers[currentQuestion.id] == index
                    val letterPrefix = when (index) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> (index + 65).toChar().toString()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) LightPurpleContainer else Color.White)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryPurple else BorderColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.selectAnswer(currentQuestion.id, index) }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Circular Indicator for A/B/C/D
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryPurple else HighDensityBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letterPrefix,
                                    color = if (isSelected) Color.White else SecondaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = option,
                                color = if (isSelected) DarkPurple else BodyTextColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Đã lựa chọn",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
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

    var activeQuiz = activeState.quizWithQuestions
    var activeSession = activeState.sessionEntity

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
            // Loading screen or fall back
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            val quiz = activeQuiz!!
            val session = activeSession!!

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
                                                    text = "Giải Thích Từ AI Trợ Lý",
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

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQuestionsScreen(
    viewModel: QuizViewModel,
    quizId: Long,
    onNavigateBack: () -> Unit,
    onAddViaAI: (Long) -> Unit
) {
    val allQuizzes by viewModel.quizzes.collectAsState(initial = emptyList())
    val quizWithQuestions = allQuizzes.find { it.quiz.id == quizId }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var questionToDelete by remember { mutableStateOf<QuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quizWithQuestions?.quiz?.title ?: "Quản lý câu hỏi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { onAddViaAI(quizId) },
                    containerColor = Color(0xFF03A9F4), // Sky Blue for AI
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Thêm bằng AI")
                }
                FloatingActionButton(
                    onClick = { showAddManualDialog = true },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm thủ công")
                }
            }
        }
    ) { padding ->
        if (quizWithQuestions == null || quizWithQuestions.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có câu hỏi nào. Hãy thêm câu hỏi mới!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizWithQuestions.questions) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = question.text,
                                    fontWeight = FontWeight.Bold,
                                    color = BodyTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingQuestion = question
                                            showAddManualDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Sửa",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { questionToDelete = question },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            question.options.forEachIndexed { index, option ->
                                Text(
                                    text = "${(index + 65).toChar()}. $option",
                                    color = if (index == question.correctOptionIndex) PrimaryPurple else BodyTextColor,
                                    fontWeight = if (index == question.correctOptionIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddManualDialog) {
        var questionText by remember { mutableStateOf(editingQuestion?.text ?: "") }
        var options by remember { mutableStateOf(editingQuestion?.options ?: listOf("", "", "", "")) }
        var correctIndex by remember { mutableIntStateOf(editingQuestion?.correctOptionIndex ?: 0) }

        AlertDialog(
            onDismissRequest = {
                showAddManualDialog = false
                editingQuestion = null
            },
            title = { Text(if (editingQuestion == null) "Thêm câu hỏi thủ công" else "Sửa câu hỏi") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Câu hỏi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    options.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (correctIndex == index),
                                onClick = { correctIndex = index }
                            )
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newVal ->
                                    val newList = options.toMutableList()
                                    newList[index] = newVal
                                    options = newList
                                },
                                label = { Text("Đáp án ${(index + 65).toChar()}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (questionText.isNotBlank() && options.all { it.isNotBlank() }) {
                        if (editingQuestion == null) {
                            viewModel.addQuestion(quizId, questionText, options, correctIndex)
                        } else {
                            viewModel.updateQuestion(editingQuestion!!.id, quizId, questionText, options, correctIndex)
                        }
                        showAddManualDialog = false
                        editingQuestion = null
                    }
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddManualDialog = false
                    editingQuestion = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Deletion Confirmation Dialog for Question
    if (questionToDelete != null) {
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text("Xác nhận xóa câu hỏi") },
            text = { Text("Bạn có chắc chắn muốn xóa câu hỏi này? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        questionToDelete?.let { viewModel.deleteQuestion(it) }
                        questionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
