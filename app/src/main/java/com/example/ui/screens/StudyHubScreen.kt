package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(
    reviewViewModel: ReviewViewModel,
    onBack: () -> Unit,
    onPractice: () -> Unit,
    onDueReview: () -> Unit,
    onMistakes: () -> Unit
) {
    val state by reviewViewModel.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, reviewViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reviewViewModel.refresh()
        }
        owner.lifecycle.addObserver(observer)
        reviewViewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(onBack = onBack)
    val dueCount = state.entries.count { it.due(state.now) }
    val mistakeCount = state.entries.count { (it.progress?.mistakeCount ?: 0) > 0 }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Học & ôn tập") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("study_hub"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hôm nay bạn muốn học gì?", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Luyện để cải thiện kỹ năng, ôn để ghi nhớ lâu hơn.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(onClick = onPractice, modifier = Modifier.fillMaxWidth().testTag("study_practice"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Psychology, null)
                            Text("Luyện điểm yếu", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronRight, null)
                        }
                        Text("Cải thiện kỹ năng còn yếu với câu hỏi và độ khó phù hợp kết quả của bạn.")
                        Text("Xem bản đồ kiến thức và chọn kỹ năng cần luyện",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(onClick = onDueReview, modifier = Modifier.fillMaxWidth().testTag("study_due_review"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Schedule, null)
                            Text("Ôn lặp lại ngắt quãng", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronRight, null)
                        }
                        Text(if (!state.loaded) "Đang tải lịch ôn…" else if (dueCount == 0)
                            "Bạn đã ôn hết các thẻ đến hạn. Có thể chọn thêm câu để ôn."
                            else "$dueCount thẻ đang chờ bạn ôn")
                        Text("Lật thẻ và đánh giá mức ghi nhớ để hẹn lần ôn tiếp theo.")
                    }
                }
            }
            item {
                ReviewReminderSettings()
            }
            item {
                OutlinedCard(onClick = onMistakes,
                    modifier = Modifier.fillMaxWidth().testTag("study_mistakes"),
                    shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sổ câu sai", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(if (state.loaded) "$mistakeCount câu từng sai • Ghi chú và xem lại lỗi"
                                else "Xem lại lỗi và ghi chú cách hiểu",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}
