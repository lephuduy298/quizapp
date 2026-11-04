package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learning.ReviewRating
import com.example.ui.ReviewEntry
import com.example.ui.ReviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun reviewDate(time: Long) = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(time))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedReviewScreen(vm: ReviewViewModel, onBack: () -> Unit, onStudied: () -> Unit = {},
    initialTab: Int = 0) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var search by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf<String?>(null) }
    var skillId by rememberSaveable { mutableStateOf<Long?>(null) }
    var quizId by rememberSaveable { mutableStateOf<Long?>(null) }
    var queue by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var position by rememberSaveable { mutableIntStateOf(0) }
    var reviewed by rememberSaveable { mutableIntStateOf(0) }
    var studying by rememberSaveable { mutableStateOf(false) }
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler { if (editorId != null) editorId = null else if (studying) studying = false else onBack() }
    val scoped = state.entries.filter { entry ->
        (subject == null || entry.skill?.subject == subject || (entry.skill == null && entry.quiz?.topic == subject)) &&
        (skillId == null || entry.question.skillId == skillId) && (quizId == null || entry.question.quizId == quizId) &&
        (search.isBlank() || entry.question.text.contains(search, true) || entry.progress?.note?.contains(search, true) == true)
    }
    val visible = scoped.filter { when (tab) {
        0 -> it.due(state.now)
        1 -> (it.progress?.mistakeCount ?: 0) > 0
        2 -> it.progress?.isFavorite == true
        else -> true
    } }.sortedWith(compareBy<ReviewEntry> { it.progress?.nextReviewAt ?: Long.MAX_VALUE }.thenBy { it.question.id })

    Scaffold(topBar = { TopAppBar(title = { Text(if (editorId != null) "Ghi chú câu hỏi" else if (studying) "Ôn thẻ ghi nhớ" else when (tab) {
        1 -> "Sổ câu sai"
        2 -> "Câu quan trọng"
        3 -> "Tất cả câu hỏi"
        else -> "Ôn lặp lại ngắt quãng"
    }) },
        navigationIcon = { IconButton(onClick = { if (editorId != null) editorId = null else if (studying) studying = false else onBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            error?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp)) {
                        Text(message, Modifier.weight(1f))
                        TextButton(onClick = { vm.error.value = null }) { Text("Đóng") }
                    }
                }
            }
            if (!state.loaded) CircularProgressIndicator(Modifier.padding(24.dp))
            else if (editorId != null) {
                val entry = state.entries.find { it.question.id == editorId }
                if (entry == null) Column(Modifier.padding(24.dp)) {
                    Text("Câu hỏi đã bị xóa.")
                    TextButton(onClick = { editorId = null }) { Text("Về danh sách") }
                } else key(entry.question.id) {
                    ReviewNoteEditor(entry, busy, onCancel = { editorId = null },
                        onSave = { note, favorite -> vm.annotate(entry.question.id, note, favorite) { editorId = null } })
                }
            } else if (studying) {
                val entry = queue.getOrNull(position)?.let { id -> state.entries.find { it.question.id == id } }
                if (position >= queue.size) Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Đã hoàn thành phiên ôn!", style = MaterialTheme.typography.headlineSmall)
                    Text("Đã lưu lịch ôn cho $reviewed thẻ. Thẻ Chưa nhớ sẽ đến hạn lại sau 10 phút.")
                    Button(onClick = { studying = false }) { Text("Về danh sách") }
                } else if (entry == null) Column(Modifier.padding(24.dp)) {
                    Text("Câu hỏi đã bị xóa hoặc không còn hợp lệ.")
                    Button(onClick = { position++ }) { Text("Bỏ qua thẻ") }
                } else key(position, entry.question.id, entry.question.contentVersion) {
                    ReviewCard(entry, position, queue.size, busy,
                        onEdit = { editorId = entry.question.id },
                        onRate = { rating, token -> vm.rate(entry, rating, token) { onStudied(); reviewed++; position++ } })
                }
            } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("review_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Cần ôn hôm nay", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${state.entries.count { it.due(state.now) }} thẻ đến hạn • ${state.entries.count { (it.progress?.mistakeCount ?: 0) > 0 }} câu từng sai")
                            Text("Tiến độ riêng theo tài khoản, lưu trên thiết bị. Lật thẻ rồi tự đánh giá để đặt lịch ôn tiếp theo.")
                        }
                    }
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Đến hạn", "Câu sai", "Quan trọng", "Tất cả").forEachIndexed { index, label ->
                            FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) })
                        }
                    }
                    OutlinedTextField(search, { search = it }, label = { Text("Tìm câu hỏi / ghi chú") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val subjects = state.entries.mapNotNull { it.skill?.subject ?: it.quiz?.topic }.filter { it.isNotBlank() }.distinct().sorted()
                    ReviewFilter("Môn", subject, subjects.map { it to it }) { subject = it; skillId = null }
                    val skills = state.entries.mapNotNull { it.skill }.distinctBy { it.id }.filter { subject == null || it.subject == subject }
                    ReviewFilter("Kỹ năng", skillId, skills.map { it.id to it.name }) { skillId = it }
                    ReviewFilter("Bộ đề", quizId, state.entries.mapNotNull { it.quiz }.distinctBy { it.id }.map { it.id to it.title }) { quizId = it }
                    Button(onClick = { queue = visible.map { it.question.id }; position = 0; reviewed = 0; studying = true },
                        enabled = visible.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Ôn danh sách này (${visible.size} thẻ)")
                    }
                }
                if (visible.isEmpty()) item { Text(when (tab) {
                    0 -> "Chưa có thẻ đến hạn. Hãy làm bài hoặc vào Tất cả để bắt đầu ôn."
                    1 -> "Chưa có câu sai trong phạm vi này. Câu sai được ghi từ thi, luyện tập và luyện thích ứng."
                    2 -> "Chưa đánh dấu câu quan trọng. Chạm một câu để thêm ghi chú và đánh dấu."
                    else -> "Không có câu hỏi phù hợp. Hãy thêm đề hoặc bỏ bộ lọc."
                }, Modifier.padding(vertical = 16.dp)) }
                items(visible, key = { it.question.id }) { entry ->
                    Card(Modifier.fillMaxWidth().clickable { editorId = entry.question.id }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(entry.question.text, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (entry.progress?.isFavorite == true) Icon(Icons.Default.Star, "Câu quan trọng")
                            }
                            Text("${entry.quiz?.title ?: ""} • ${entry.skill?.name ?: "Chưa gắn kỹ năng"}", style = MaterialTheme.typography.bodySmall)
                            entry.progress?.let { p ->
                                if (p.mistakeCount > 0) Text("Đã sai ${p.mistakeCount} lần", color = MaterialTheme.colorScheme.error)
                                Text(p.nextReviewAt?.let { if (entry.due(state.now)) "Đã đến hạn ôn" else "Ôn tiếp: ${reviewDate(it)}" } ?: "Chưa có lịch ôn")
                                if (p.note.isNotBlank()) Text(p.note, maxLines = 2)
                            }
                            if (entry.changed) Text("Nội dung đã đổi: lịch ôn bắt đầu lại, giữ ghi chú và đánh dấu.")
                            TextButton(onClick = { queue = listOf(entry.question.id); position = 0; reviewed = 0; studying = true }) { Text("Ôn câu này") }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ReviewNoteEditor(entry: ReviewEntry, busy: Boolean, onCancel: () -> Unit,
    onSave: (String, Boolean) -> Unit) {
    var note by rememberSaveable { mutableStateOf(entry.progress?.note ?: "") }
    var favorite by rememberSaveable { mutableStateOf(entry.progress?.isFavorite ?: false) }
    Column(Modifier.fillMaxSize().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(entry.question.text, style = MaterialTheme.typography.titleLarge)
            entry.progress?.lastSelectedOption?.let { index ->
                Text("Lần sai gần nhất bạn chọn: ${entry.question.options.getOrNull(index) ?: ""}", color = MaterialTheme.colorScheme.error) }
            Text("Đáp án đúng: ${entry.question.options[entry.question.correctOptionIndex]}")
            OutlinedTextField(note, { if (it.length <= 5000) note = it }, label = { Text("Cách hiểu / ghi nhớ") },
                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("review_note"), minLines = 3, maxLines = 8)
            Text("${note.length}/5000 ký tự", style = MaterialTheme.typography.bodySmall)
            Row {
                Checkbox(favorite, { favorite = it }, enabled = !busy)
                Text("Đánh dấu quan trọng", Modifier.padding(top = 12.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !busy) { Text("Hủy") }
            Button(onClick = { onSave(note, favorite) }, enabled = !busy) { Text("Lưu ghi chú") }
        }
    }
}

@Composable
private fun ReviewCard(entry: ReviewEntry, position: Int, total: Int, busy: Boolean,
    onEdit: () -> Unit, onRate: (ReviewRating, String) -> Unit) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val token = rememberSaveable { "card:${UUID.randomUUID()}" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Thẻ ${position + 1}/$total • ${entry.skill?.name ?: entry.quiz?.topic ?: ""}")
        LinearProgressIndicator(progress = { position.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth().testTag("review_question")) {
            Text(entry.question.text, Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        }
        if (!revealed) Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Hiện đáp án") }
        else {
            Text("Đáp án đúng", fontWeight = FontWeight.Bold)
            Text(entry.question.options[entry.question.correctOptionIndex], style = MaterialTheme.typography.titleMedium)
            entry.progress?.note?.takeIf { it.isNotBlank() }?.let { Text("Ghi chú: $it") }
            Text("Bạn nhớ câu này ở mức nào? Đây là tự đánh giá, không tính thành điểm trắc nghiệm.")
            ReviewRating.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { rating -> OutlinedButton(onClick = { onRate(rating, token) }, enabled = !busy,
                        modifier = Modifier.weight(1f)) { Text(rating.label) } }
                }
            }
            Text("Chưa nhớ: 10 phút • Khó: 1 ngày • Nhớ/Dễ: tăng dần theo mốc 1, 3, 7, 14, 30 ngày.", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onEdit, enabled = !busy) { Text("Ghi chú / đánh dấu quan trọng") }
    }
}

@Composable
private fun <T> ReviewFilter(label: String, selected: T?, choices: List<Pair<T, String>>, onSelect: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("$label: ${choices.find { it.first == selected }?.second ?: "Tất cả"}") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Tất cả") }, onClick = { onSelect(null); expanded = false })
            choices.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false }) }
        }
    }
}
