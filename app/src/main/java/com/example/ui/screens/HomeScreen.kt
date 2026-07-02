package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.FolderEntity
import com.example.data.local.QuizSessionEntity
import com.example.data.local.QuizWithQuestions
import com.example.ui.QuizGenerationState
import com.example.ui.QuizViewModel
import com.example.ui.QuizMode
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.*
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: QuizViewModel,
    onStartQuiz: (QuizWithQuestions, QuizMode) -> Unit,
    onStartFlashcards: (QuizWithQuestions) -> Unit,
    onEditQuiz: (QuizWithQuestions) -> Unit,
    onViewSession: (QuizWithQuestions, Long) -> Unit,
    onNavigateToCamera: (Long?) -> Unit
) {
    val quizzes by viewModel.quizzes.collectAsState(initial = emptyList())
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    val folders by viewModel.folders.collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    
    val streakCount by viewModel.streakCount.collectAsState()
    var showStreakDialog by remember { mutableStateOf(false) }
    
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    var showReminderDialog by remember { mutableStateOf(false) }
    
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    val activeFolderId = remember(selectedFolderId) {
        if (selectedFolderId == -1L) null else selectedFolderId
    }
    
    val filteredQuizzes = remember(quizzes, selectedFolderId) {
        when (selectedFolderId) {
            null -> quizzes
            -1L -> quizzes.filter { it.quiz.folderId == null }
            else -> quizzes.filter { it.quiz.folderId == selectedFolderId }
        }
    }
    
    val userInitials = currentUser?.trim()?.take(2)?.uppercase() ?: "AI"
    var showQuickCreateDialog by remember { mutableStateOf(false) }
    var showAIChatDialog by remember { mutableStateOf(false) }
    var showAICreationSelection by remember { mutableStateOf(false) }
    var quizToDelete by remember { mutableStateOf<QuizWithQuestions?>(null) }
    var quizToStart by remember { mutableStateOf<QuizWithQuestions?>(null) }

    LaunchedEffect(generationState) {
        if (generationState is QuizGenerationState.Success) {
            viewModel.resetGenerationState()
            showAIChatDialog = false
        }
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "QuizAI",
                                fontWeight = FontWeight.Bold,
                                color = BodyTextColor,
                                fontSize = 18.sp
                            )
                            // Elegant Gradient AI Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(PrimaryPurple, DarkPurple)
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "AI",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Tài khoản: ${currentUser ?: "Khách"}",
                            fontSize = 12.sp,
                            color = SecondaryTextColor
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreakFlameWidget(
                        streakCount = streakCount,
                        onClick = { showStreakDialog = true }
                    )

                    IconButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LightPurpleContainer)
                            .testTag("reminder_button")
                    ) {
                        Icon(
                            imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Nhắc nhở học tập",
                            tint = DarkPurple,
                            modifier = Modifier.size(20.dp)
                        )
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
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Main AI Creation Button
                ExtendedFloatingActionButton(
                    onClick = { showAICreationSelection = true },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text("Tạo Đề Bằng AI", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("main_ai_fab")
                )

                // Quick Manual Create (Small icon only)
                FloatingActionButton(
                    onClick = { showQuickCreateDialog = true },
                    containerColor = LightPurpleContainer,
                    contentColor = DarkPurple,
                    modifier = Modifier.size(48.dp).testTag("manual_create_fab")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Sổ đề thủ công")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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

                        // Token / Status Card -> AI Learning Assistant Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(LightPurpleContainer)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = DarkPurple,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "TRỢ LÝ HỌC TẬP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkPurple,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Trợ Lý AI",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DarkPurple
                                    )
                                    // Small green indicator dot for "Active/Online"
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tạo đề & giải nghĩa 24/7",
                                    fontSize = 11.sp,
                                    color = DarkPurple.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // ANALYTICS DASHBOARD
                item {
                    AnalyticsDashboard(sessions = sessions)
                }

                // FOLDER CHIPS ROW
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "THƯ MỤC HỌC TẬP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTextColor,
                                letterSpacing = 1.sp
                            )
                            
                            // Management options for currently selected custom folder
                            if (selectedFolderId != null && selectedFolderId != -1L) {
                                val currentFolder = folders.find { it.id == selectedFolderId }
                                if (currentFolder != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        var showRenameDialog by remember { mutableStateOf(false) }
                                        var showDeleteDialog by remember { mutableStateOf(false) }
                                        
                                        IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Đổi tên thư mục", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa thư mục", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                        }
                                        
                                        if (showRenameDialog) {
                                            CreateEditFolderDialog(
                                                editingFolder = currentFolder,
                                                onDismiss = { showRenameDialog = false },
                                                onSave = { name ->
                                                    viewModel.updateFolder(currentFolder.id, name)
                                                    showRenameDialog = false
                                                }
                                            )
                                        }
                                        
                                        if (showDeleteDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteDialog = false },
                                                title = { Text("Xác nhận xóa thư mục") },
                                                text = { Text("Bạn có chắc chắn muốn xóa thư mục '${currentFolder.name}'? Các đề thi bên trong sẽ được chuyển sang danh sách 'Chưa phân loại'.") },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            viewModel.deleteFolder(currentFolder.id)
                                                            selectedFolderId = null // Go back to All
                                                            showDeleteDialog = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                                    ) {
                                                        Text("Xóa")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteDialog = false }) {
                                                        Text("Hủy")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Horizontal list of chips
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Chip: All
                            item {
                                val isSelected = selectedFolderId == null
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolderId = null },
                                    label = { Text("Tất cả (${quizzes.size})") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            
                            // Chip: Uncategorized
                            item {
                                val uncategorizedCount = quizzes.count { it.quiz.folderId == null }
                                val isSelected = selectedFolderId == -1L
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolderId = -1L },
                                    label = { Text("Chưa phân loại ($uncategorizedCount)") },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            // Chips: User folders
                            items(folders) { folder ->
                                val quizCount = quizzes.count { it.quiz.folderId == folder.id }
                                val isSelected = selectedFolderId == folder.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFolderId = folder.id },
                                    label = { Text("${folder.name} ($quizCount)") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            // Chip: Add new folder
                            item {
                                var showCreateDialog by remember { mutableStateOf(false) }
                                AssistChip(
                                    onClick = { showCreateDialog = true },
                                    label = { Text("Thư mục mới") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                
                                if (showCreateDialog) {
                                    CreateEditFolderDialog(
                                        editingFolder = null,
                                        onDismiss = { showCreateDialog = false },
                                        onSave = { name ->
                                            viewModel.createFolder(name)
                                            showCreateDialog = false
                                        }
                                    )
                                }
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
                            text = "DANH SÁCH ĐỀ THI (${filteredQuizzes.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTextColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // LIST OF QUIZZES
                if (filteredQuizzes.isEmpty()) {
                    item {
                        CardEmptyState(
                            title = "Chưa có đề thi nào trong thư mục này",
                            subtitle = "Chụp một tài liệu bài tập hoặc ra lệnh cho AI Chat soạn đề giúp bạn để bắt đầu học tập.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    itemsIndexed(filteredQuizzes) { index, item ->
                        QuizDensityRow(
                            quizWithQuestions = item,
                            onStart = { quizToStart = item },
                            onEdit = { onEditQuiz(item) },
                            onDelete = { quizToDelete = item },
                            folders = folders,
                            onMoveToFolder = { targetFolderId ->
                                viewModel.moveQuizToFolder(item.quiz.id, targetFolderId)
                            }
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
                            subtitle = "Khi bạn hoàn thành đề thi và bấm Nộp bài, các kết quả sẽ hiển thị tại đây.",
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

            // PROCESSING OVERLAY
            if (generationState !is QuizGenerationState.Idle) {
                AIGenerationOverlay(
                    state = generationState,
                    onReset = { viewModel.resetGenerationState() }
                )
            }
        }
    }

    if (showReminderDialog) {
        StudyReminderDialog(
            viewModel = viewModel,
            onDismiss = { showReminderDialog = false }
        )
    }

    // Streak Congratulatory Dialog
    if (showStreakDialog) {
        Dialog(onDismissRequest = { showStreakDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pulsing Flame in Dialog
                    val infiniteTransition = rememberInfiniteTransition(label = "DialogFlamePulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "PulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Outer Path
                            val outerPath = Path().apply {
                                moveTo(w * 0.5f, h * 0.05f)
                                cubicTo(w * 0.65f, h * 0.2f, w * 0.9f, h * 0.45f, w * 0.85f, h * 0.72f)
                                cubicTo(w * 0.8f, h * 0.92f, w * 0.65f, h * 0.98f, w * 0.5f, h * 0.98f)
                                cubicTo(w * 0.35f, h * 0.98f, w * 0.2f, h * 0.92f, w * 0.15f, h * 0.72f)
                                cubicTo(w * 0.1f, h * 0.45f, w * 0.35f, h * 0.2f, w * 0.5f, h * 0.05f)
                                close()
                            }
                            drawPath(
                                path = outerPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                )
                            )
                            
                            // Middle Path
                            val midPath = Path().apply {
                                moveTo(w * 0.5f, h * 0.25f)
                                cubicTo(w * 0.6f, h * 0.38f, w * 0.78f, h * 0.55f, w * 0.74f, h * 0.75f)
                                cubicTo(w * 0.7f, h * 0.9f, w * 0.6f, h * 0.94f, w * 0.5f, h * 0.94f)
                                cubicTo(w * 0.4f, h * 0.94f, w * 0.3f, h * 0.9f, w * 0.26f, h * 0.75f)
                                cubicTo(w * 0.22f, h * 0.55f, w * 0.4f, h * 0.38f, w * 0.5f, h * 0.25f)
                                close()
                            }
                            drawPath(
                                path = midPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF9100), Color(0xFFFFEA00))
                                )
                            )
                            
                            // Inner Path
                            val innerPath = Path().apply {
                                moveTo(w * 0.5f, h * 0.48f)
                                cubicTo(w * 0.56f, h * 0.56f, w * 0.66f, h * 0.68f, w * 0.63f, h * 0.8f)
                                cubicTo(w * 0.6f, h * 0.88f, w * 0.55f, h * 0.9f, w * 0.5f, h * 0.9f)
                                cubicTo(w * 0.45f, h * 0.9f, w * 0.40f, h * 0.88f, w * 0.37f, h * 0.8f)
                                cubicTo(w * 0.34f, h * 0.68f, w * 0.44f, h * 0.56f, w * 0.5f, h * 0.48f)
                                close()
                            }
                            drawPath(
                                path = innerPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFEA00), Color(0xFFFFFFFF))
                                )
                            )
                        }
                    }

                    Text(
                        text = "Chuỗi Học Tập 🔥",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BodyTextColor
                    )

                    Text(
                        text = if (streakCount > 0) {
                            "Bạn đã học liên tục $streakCount ngày! Hãy tiếp tục giữ lửa nhé! 🔥"
                        } else {
                            "Hãy hoàn thành bài học đầu tiên hôm nay để bắt đầu chuỗi học tập! 🚀"
                        },
                        fontSize = 15.sp,
                        color = BodyTextColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )

                    Divider(color = BorderColor, thickness = 1.dp)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Luật duy trì chuỗi:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryPurple
                        )
                        BulletPointText("Hoàn thành ít nhất một hoạt động học tập mỗi ngày.")
                        BulletPointText("Các hoạt động hợp lệ: Thi thử đề thi, Luyện tập trả lời câu hỏi, hoặc hoàn thành ôn tập một bộ Flashcard.")
                        BulletPointText("Nếu qua một ngày không có hoạt động nào, chuỗi ngày sẽ tự động reset về 0.")
                    }

                    Button(
                        onClick = { showStreakDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Tuyệt vời, tiếp tục thôi!", fontWeight = FontWeight.Bold)
                    }
                }
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
                                    viewModel.createQuickQuiz(
                                        title = title,
                                        topic = topic,
                                        duration = durationVal,
                                        questionsList = emptyList(),
                                        folderId = activeFolderId
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

    if (showAIChatDialog) {
        AIChatGenerationDialog(
            onDismiss = { showAIChatDialog = false },
            onGenerate = { prompt ->
                viewModel.generateQuizFromText(prompt, folderId = activeFolderId)
                showAIChatDialog = false
            }
        )
    }

    // AI Creation Selection Dialog
    if (showAICreationSelection) {
        AICreationSelectionDialog(
            onDismiss = { showAICreationSelection = false },
            onSelectChat = {
                showAICreationSelection = false
                showAIChatDialog = true
            },
            onSelectCamera = {
                showAICreationSelection = false
                onNavigateToCamera(activeFolderId)
            }
        )
    }

    // Mode Selection Dialog
    if (quizToStart != null) {
        Dialog(onDismissRequest = { quizToStart = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Chọn chế độ học tập",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BodyTextColor
                    )
                    
                    Text(
                        text = "Đề bài: ${quizToStart?.quiz?.title}",
                        fontSize = 13.sp,
                        color = SecondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Practice Mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val quiz = quizToStart!!
                                    quizToStart = null
                                    onStartQuiz(quiz, QuizMode.PRACTICE)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LightPurpleContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = DarkPurple, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Luyện tập", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                                    Text("Không hẹn giờ, xem đáp án & giải thích AI ngay lập tức", fontSize = 11.sp, color = DarkPurple.copy(alpha = 0.8f))
                                }
                            }
                        }

                        // Option 2: Exam Mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val quiz = quizToStart!!
                                    quizToStart = null
                                    onStartQuiz(quiz, QuizMode.EXAM)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = CorrectGreen, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Thi thử", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CorrectGreen)
                                    Text("Có đếm ngược thời gian, chấm điểm sau khi nộp bài", fontSize = 11.sp, color = CorrectGreen.copy(alpha = 0.8f))
                                }
                            }
                        }

                        // Option 3: Flashcard Mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val quiz = quizToStart!!
                                    quizToStart = null
                                    onStartFlashcards(quiz)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Học Flashcard 🎴", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                    Text("Học bằng thẻ ghi nhớ xoay lật 3D trực quan, kèm AI", fontSize = 11.sp, color = Color(0xFFE65100).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                    
                    TextButton(onClick = { quizToStart = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Hủy bỏ")
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsDashboard(
    sessions: List<QuizSessionEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BIỂU ĐỒ HIỆU SUẤT HỌC TẬP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Làm bài thi đầu tiên để theo dõi tiến độ",
                        color = SecondaryTextColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val avgScore = sessions.map { it.score }.average()
                val totalSessions = sessions.size
                val passRate = (sessions.count { it.score >= 50.0 }.toDouble() / totalSessions * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Điểm trung bình", fontSize = 10.sp, color = SecondaryTextColor)
                        Text("${avgScore.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BodyTextColor)
                    }
                    Column {
                        Text("Tổng số bài thi", fontSize = 10.sp, color = SecondaryTextColor)
                        Text("$totalSessions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BodyTextColor)
                    }
                    Column {
                        Text("Tỉ lệ Đạt", fontSize = 10.sp, color = SecondaryTextColor)
                        Text("$passRate%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (passRate >= 50) CorrectGreen else ErrorRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val lastAttempts = sessions.take(10).reversed()
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val y100 = 0f
                    val y50 = canvasHeight / 2
                    val y0 = canvasHeight
                    
                    drawLine(Color(0xFFE0E0E0), start = androidx.compose.ui.geometry.Offset(0f, y100), end = androidx.compose.ui.geometry.Offset(canvasWidth, y100), strokeWidth = 1f)
                    drawLine(Color(0xFFF0F0F0), start = androidx.compose.ui.geometry.Offset(0f, y50), end = androidx.compose.ui.geometry.Offset(canvasWidth, y50), strokeWidth = 1f)
                    drawLine(Color(0xFFE0E0E0), start = androidx.compose.ui.geometry.Offset(0f, y0), end = androidx.compose.ui.geometry.Offset(canvasWidth, y0), strokeWidth = 1f)

                    if (lastAttempts.size > 1) {
                        val spaceX = canvasWidth / (lastAttempts.size - 1)
                        val points = lastAttempts.mapIndexed { index, session ->
                            val x = index * spaceX
                            val y = canvasHeight - (session.score.toFloat() / 100f * canvasHeight)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = PrimaryPurple,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, canvasHeight)
                            for (i in 0 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, canvasHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(PrimaryPurple.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = canvasHeight
                            )
                        )

                        points.forEachIndexed { index, point ->
                            drawCircle(
                                color = PrimaryPurple,
                                radius = 6f,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = point
                            )
                        }
                    } else if (lastAttempts.size == 1) {
                        val x = canvasWidth / 2
                        val y = canvasHeight - (lastAttempts[0].score.toFloat() / 100f * canvasHeight)
                        drawCircle(
                            color = PrimaryPurple,
                            radius = 6f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizDensityRow(
    quizWithQuestions: QuizWithQuestions,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    folders: List<FolderEntity>,
    onMoveToFolder: (Long?) -> Unit
) {
    var showMoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                IconButton(onClick = { showMoveDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Di chuyển thư mục",
                        tint = PrimaryPurple.copy(alpha = 0.8f)
                    )
                }
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

    if (showMoveDialog) {
        MoveQuizToFolderDialog(
            currentFolderId = quizWithQuestions.quiz.folderId,
            folders = folders,
            onDismiss = { showMoveDialog = false },
            onSelectFolder = { targetFolderId ->
                onMoveToFolder(targetFolderId)
                showMoveDialog = false
            }
        )
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

@Composable
fun MoveQuizToFolderDialog(
    currentFolderId: Long?,
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onSelectFolder: (Long?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Di chuyển vào thư mục",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BodyTextColor
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)
                ) {
                    // Option 1: Uncategorized (Chưa phân loại)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(null) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentFolderId == null) LightPurpleContainer else Color.White
                            ),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = SecondaryTextColor)
                                Text("Chưa phân loại", fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Options: User Folders
                    items(folders) { folder ->
                        val isSelected = currentFolderId == folder.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(folder.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) LightPurpleContainer else Color.White
                            ),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryPurple)
                                Text(folder.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditFolderDialog(
    editingFolder: FolderEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var folderName by remember { mutableStateOf(editingFolder?.name ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (editingFolder == null) "Tạo thư mục mới" else "Sửa tên thư mục",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BodyTextColor
                )
                
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Tên thư mục") },
                    placeholder = { Text("Nhập tên thư mục học tập...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (folderName.isNotBlank()) {
                                onSave(folderName.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Lưu")
                    }
                }
            }
        }
    }
}

@Composable
fun StreakFlameWidget(
    streakCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StreakFlamePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw outer layer
                val outerPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.05f)
                    cubicTo(w * 0.65f, h * 0.2f, w * 0.9f, h * 0.45f, w * 0.85f, h * 0.72f)
                    cubicTo(w * 0.8f, h * 0.92f, w * 0.65f, h * 0.98f, w * 0.5f, h * 0.98f)
                    cubicTo(w * 0.35f, h * 0.98f, w * 0.2f, h * 0.92f, w * 0.15f, h * 0.72f)
                    cubicTo(w * 0.1f, h * 0.45f, w * 0.35f, h * 0.2f, w * 0.5f, h * 0.05f)
                    close()
                }
                drawPath(
                    path = outerPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                    )
                )

                // Draw middle layer
                val midPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.25f)
                    cubicTo(w * 0.6f, h * 0.38f, w * 0.78f, h * 0.55f, w * 0.74f, h * 0.75f)
                    cubicTo(w * 0.7f, h * 0.9f, w * 0.6f, h * 0.94f, w * 0.5f, h * 0.94f)
                    cubicTo(w * 0.4f, h * 0.94f, w * 0.3f, h * 0.9f, w * 0.26f, h * 0.75f)
                    cubicTo(w * 0.22f, h * 0.55f, w * 0.4f, h * 0.38f, w * 0.5f, h * 0.25f)
                    close()
                }
                drawPath(
                    path = midPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF9100), Color(0xFFFFEA00))
                    )
                )

                // Draw inner layer
                val innerPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.48f)
                    cubicTo(w * 0.56f, h * 0.56f, w * 0.66f, h * 0.68f, w * 0.63f, h * 0.8f)
                    cubicTo(w * 0.6f, h * 0.88f, w * 0.55f, h * 0.9f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.45f, h * 0.9f, w * 0.40f, h * 0.88f, w * 0.37f, h * 0.8f)
                    cubicTo(w * 0.34f, h * 0.68f, w * 0.44f, h * 0.56f, w * 0.5f, h * 0.48f)
                    close()
                }
                drawPath(
                    path = innerPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFEA00), Color(0xFFFFFFFF))
                    )
                )
            }
        }
        Text(
            text = "$streakCount",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color(0xFFE65100)
        )
    }
}

@Composable
fun BulletPointText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 12.sp, color = SecondaryTextColor, lineHeight = 18.sp)
    }
}

@Composable
fun StudyReminderDialog(
    viewModel: QuizViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val reminderDays by viewModel.reminderDays.collectAsState()

    var tempEnabled by remember { mutableStateOf(reminderEnabled) }
    var tempHour by remember { mutableIntStateOf(reminderHour) }
    var tempMinute by remember { mutableIntStateOf(reminderMinute) }
    var tempDays by remember { mutableStateOf(reminderDays) }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempEnabled = true
            Toast.makeText(context, "Đã cấp quyền thông báo thành công!", Toast.LENGTH_SHORT).show()
        } else {
            tempEnabled = false
            Toast.makeText(
                context,
                "Bạn cần cấp quyền thông báo để nhận nhắc nhở học tập.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LightPurpleContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = DarkPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Nhắc Nhở Học Tập ⏰",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BodyTextColor
                )

                Text(
                    text = "Thiết lập thời gian học tập mỗi ngày giúp bạn duy trì chuỗi Streak và củng cố kiến thức tốt hơn!",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Divider(color = BorderColor, thickness = 1.dp)

                // Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HighDensityBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bật nhắc nhở",
                            fontWeight = FontWeight.Bold,
                            color = BodyTextColor,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (tempEnabled) "Sẽ thông báo hàng ngày" else "Đang tắt nhắc nhở",
                            color = SecondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = tempEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // Request permission on Android 13+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        tempEnabled = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    tempEnabled = true
                                }
                            } else {
                                tempEnabled = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryPurple,
                            uncheckedThumbColor = SecondaryTextColor,
                            uncheckedTrackColor = BorderColor
                        )
                    )
                }

                // Time Selector Card (Compose-Native Inline UI)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (tempEnabled) LightPurpleContainer.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (tempEnabled) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "THỜI GIAN NHẮC HỌC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tempEnabled) DarkPurple else SecondaryTextColor.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (tempEnabled) LightPurpleContainer else BorderColor.copy(alpha = 0.2f))
                                        .clickable(enabled = tempEnabled) {
                                            tempHour = (tempHour + 1) % 24
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Tăng giờ",
                                        tint = if (tempEnabled) PrimaryPurple else SecondaryTextColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (tempEnabled) Color.White else BorderColor.copy(alpha = 0.1f))
                                        .border(1.dp, if (tempEnabled) PrimaryPurple.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", tempHour),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (tempEnabled) DarkPurple else SecondaryTextColor.copy(alpha = 0.5f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (tempEnabled) LightPurpleContainer else BorderColor.copy(alpha = 0.2f))
                                        .clickable(enabled = tempEnabled) {
                                            tempHour = (tempHour + 23) % 24
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Giảm giờ",
                                        tint = if (tempEnabled) PrimaryPurple else SecondaryTextColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = ":",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tempEnabled) PrimaryPurple else SecondaryTextColor.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            
                            // Minute selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (tempEnabled) LightPurpleContainer else BorderColor.copy(alpha = 0.2f))
                                        .clickable(enabled = tempEnabled) {
                                            tempMinute = (tempMinute + 1) % 60
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Tăng phút",
                                        tint = if (tempEnabled) PrimaryPurple else SecondaryTextColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (tempEnabled) Color.White else BorderColor.copy(alpha = 0.1f))
                                        .border(1.dp, if (tempEnabled) PrimaryPurple.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", tempMinute),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (tempEnabled) DarkPurple else SecondaryTextColor.copy(alpha = 0.5f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (tempEnabled) LightPurpleContainer else BorderColor.copy(alpha = 0.2f))
                                        .clickable(enabled = tempEnabled) {
                                            tempMinute = (tempMinute + 59) % 60
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Giảm phút",
                                        tint = if (tempEnabled) PrimaryPurple else SecondaryTextColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Days of the Week Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Các ngày nhắc học trong tuần:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tempEnabled) BodyTextColor else SecondaryTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        val daysOfWeek = listOf(
                            Calendar.MONDAY to "T2",
                            Calendar.TUESDAY to "T3",
                            Calendar.WEDNESDAY to "T4",
                            Calendar.THURSDAY to "T5",
                            Calendar.FRIDAY to "T6",
                            Calendar.SATURDAY to "T7",
                            Calendar.SUNDAY to "CN"
                        )
                        
                        daysOfWeek.forEach { (dayInt, label) ->
                            val isSelected = tempDays.contains(dayInt)
                            val animateBgColor by animateColorAsState(
                                targetValue = when {
                                    !tempEnabled -> BorderColor.copy(alpha = 0.2f)
                                    isSelected -> PrimaryPurple
                                    else -> HighDensityBackground
                                },
                                label = "bgColor"
                            )
                            val animateTextColor by animateColorAsState(
                                targetValue = when {
                                    !tempEnabled -> SecondaryTextColor.copy(alpha = 0.4f)
                                    isSelected -> Color.White
                                    else -> SecondaryTextColor
                                },
                                label = "textColor"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(animateBgColor)
                                    .clickable(enabled = tempEnabled) {
                                        tempDays = if (isSelected) {
                                            tempDays - dayInt
                                        } else {
                                            tempDays + dayInt
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = animateTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Hủy",
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryTextColor
                        )
                    }

                    Button(
                        onClick = {
                            if (tempEnabled && tempDays.isEmpty()) {
                                Toast.makeText(context, "Vui lòng chọn ít nhất một ngày trong tuần để nhận nhắc nhở!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.updateReminderSettings(tempEnabled, tempHour, tempMinute, tempDays, context)
                            val message = if (tempEnabled) {
                                String.format("Đã đặt nhắc nhở học tập vào %02d:%02d cho các ngày đã chọn!", tempHour, tempMinute)
                            } else {
                                "Đã tắt nhắc nhở học tập."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Lưu cấu hình",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
