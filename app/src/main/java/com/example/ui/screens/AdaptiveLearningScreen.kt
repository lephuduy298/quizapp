package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.learning.*
import com.example.ui.AdaptivePracticeViewModel
import com.example.ui.LearningUiState
import com.example.ui.theme.PrimaryPurple
import kotlin.random.Random

private fun difficultyName(level: Int) = when (level) { 1 -> "Dễ"; 3 -> "Khó"; else -> "Vừa" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveLearningScreen(viewModel: AdaptivePracticeViewModel, initialQuizId: Long? = null, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var practicing by rememberSaveable { mutableStateOf(false) }
    var quizId by rememberSaveable { mutableStateOf(initialQuizId) }
    var folderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var count by rememberSaveable { mutableIntStateOf(10) }
    var classify by rememberSaveable { mutableStateOf(false) }
    var selectedSkill by remember { mutableStateOf<SkillMastery?>(null) }
    var endDialog by remember { mutableStateOf(false) }
    val session = state.session
    LaunchedEffect(state.loaded, state.bank.quizzes, state.bank.folders) {
        if (state.loaded) {
            if (quizId != null && state.bank.quizzes.none { it.id == quizId }) quizId = null
            if (folderId != null && state.bank.folders.none { it.id == folderId }) folderId = null
        }
    }
    val inSession = practicing && session != null && session.completedAt == null
    BackHandler { if (classify) classify = false else if (practicing) practicing = false else onBack() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, inSession, session?.currentQuestionId, session?.answered) {
        fun update() = viewModel.timing(inSession && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> update() }
        owner.lifecycle.addObserver(observer)
        update()
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.timing(false) }
    }

    val questions = remember(state.bank, quizId, folderId) {
        val quizIds = state.bank.quizzes.filter { folderId == null || it.folderId == folderId }.map { it.id }.toSet()
        state.bank.questions.filter { (quizId == null || it.quizId == quizId) && it.quizId in quizIds }
    }
    val scopedMastery = remember(state.bank.skills, questions, state.attempts) {
        MasteryCalculator.calculate(state.bank.skills, questions, state.attempts).filter { it.availableQuestions > 0 }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (classify) "Phân loại câu hỏi" else if (inSession) "Luyện tập thích ứng" else "Bản đồ kiến thức") }, navigationIcon = {
            IconButton(onClick = { if (classify) classify = false else if (practicing) practicing = false else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
        }, actions = {
            if (inSession) TextButton(onClick = { endDialog = true }, enabled = !busy) { Text("Kết thúc") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            error?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f))
                        TextButton(onClick = { viewModel.error.value = null }) { Text("Đóng") }
                    }
                }
            }
            if (!state.loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (classify) ClassificationContent(questions, state.bank.skills, busy,
                onSave = { ids, subject, name, level -> viewModel.classify(ids, subject, name, level) { classify = false } })
            else if (inSession) PracticeContent(state, viewModel, busy)
            else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("knowledge_map_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    LearningCard {
                        Text("Học đúng phần bạn cần", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Chọn câu theo kết quả gần đây, độ khó và mức tự tin. Dữ liệu được lưu trên thiết bị.")
                        Text("${scopedMastery.count { it.status == MasteryStatus.MASTERED }} kỹ năng đã vững • ${scopedMastery.count { it.needsFocus }} cần ưu tiên")
                    }
                }
                if (session != null && session.completedAt == null) item {
                    LearningCard {
                        Text("Bạn có một phiên đang làm", fontWeight = FontWeight.Bold)
                        Text("${state.attempts.count { it.sessionId == session.id && !it.skipped }}/${session.targetCount} câu đã trả lời")
                        Button(onClick = { practicing = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Tiếp tục phiên luyện") }
                        TextButton(onClick = { endDialog = true }, enabled = !busy) { Text("Kết thúc phiên hiện tại") }
                    }
                }
                if (session?.completedAt != null) item { SessionSummary(state) }
                item {
                    LearningCard {
                        Text("Phạm vi luyện", fontWeight = FontWeight.Bold)
                        ChoiceMenu("Thư mục", folderId, listOf(null to "Tất cả thư mục") + state.bank.folders.map { it.id to it.name }) {
                            folderId = it; quizId = null
                        }
                        ChoiceMenu("Bộ đề", quizId, listOf(null to "Tất cả bộ đề") + state.bank.quizzes.filter {
                            folderId == null || it.folderId == folderId
                        }.map { it.id to it.title }) { quizId = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { n -> FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n câu") }) }
                        }
                        Text("${questions.count { it.skillId != null }} câu đã phân loại • ${questions.count { it.skillId == null }} chưa phân loại")
                        Button(onClick = { practicing = true; viewModel.start(count, quizId, folderId, null) },
                            enabled = !busy && (session == null || session.completedAt != null) && questions.any { it.skillId != null },
                            modifier = Modifier.fillMaxWidth()) { Text("Luyện điểm yếu") }
                        OutlinedButton(onClick = { classify = true }, enabled = !busy && questions.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text("Phân loại kỹ năng & độ khó")
                        }
                    }
                }
                if (scopedMastery.isEmpty()) item {
                    Text("Chưa có bản đồ. Hãy gắn kỹ năng cho câu hỏi hoặc thêm bộ đề Android mẫu.")
                }
                scopedMastery.groupBy { it.skill.subject }.forEach { (subject, entries) ->
                    item { Text(subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(entries.chunked(2)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { mastery -> SkillTile(mastery, Modifier.weight(1f)) { selectedSkill = mastery } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { viewModel.demo { quizId = null; folderId = null } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (busy) "Đang lưu…" else "Thêm bộ đề Android mẫu (18 câu)")
                    }
                    Text("Điểm thành thạo là chỉ báo từ lịch sử luyện, không phải điểm thi. Cần ít nhất 5 câu khác nhau và có câu Vừa/Khó trả lời đúng để đạt Đã vững.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    selectedSkill?.let { selected ->
        val m = scopedMastery.find { it.skill.id == selected.skill.id } ?: selected
        AlertDialog(onDismissRequest = { selectedSkill = null }, title = { Text(m.skill.name) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${m.status.label} • ${m.score}/100")
                Text("Đã luyện ${m.distinctQuestions}/${m.availableQuestions} câu khác nhau; ${m.mistakes} câu sai trong ${m.recentCount} lượt gần đây.")
                val history = MasteryCalculator.validAttempts(questions, state.attempts).filter { it.skillId == m.skill.id }.takeLast(5)
                Text("Gần đây: " + history.joinToString(" • ") { if (it.correct) "Đúng" else "Sai" }.ifEmpty { "Chưa có dữ liệu" })
                val uncertain = history.count { it.correct && it.confidence == 1 }
                val misconceptions = history.count { !it.correct && it.confidence == 3 }
                Text("$uncertain lượt đúng nhưng chưa chắc • $misconceptions lượt sai dù rất chắc")
            }
        }, confirmButton = {
            TextButton(onClick = { selectedSkill = null; practicing = true; viewModel.start(count, quizId, folderId, m.skill.id) },
                enabled = !busy && (session == null || session.completedAt != null)) { Text("Luyện kỹ năng này") }
        }, dismissButton = { TextButton(onClick = { selectedSkill = null }) { Text("Đóng") } })
    }
    if (endDialog) AlertDialog(onDismissRequest = { endDialog = false }, title = { Text("Kết thúc phiên luyện?") },
        text = { Text("Kết quả đã kiểm tra được giữ lại. Bạn có thể quay về bản đồ và tiếp tục phiên sau nếu chưa muốn kết thúc.") },
        confirmButton = { TextButton(onClick = { viewModel.finish(); endDialog = false; practicing = false }, enabled = !busy) { Text("Kết thúc") } },
        dismissButton = { TextButton(onClick = { endDialog = false }) { Text("Tiếp tục học") } })
}

@Composable
private fun LearningCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ChoiceMenu(label: String, selected: Long?, options: List<Pair<Long?, String>>, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${options.find { it.first == selected }?.second ?: "Tất cả"}", Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false }) }
        }
    }
}

@Composable
private fun SkillTile(m: SkillMastery, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(when {
        m.status == MasteryStatus.MASTERED -> Color(0xFF218C65)
        m.needsFocus -> Color(0xFFBA4B36)
        m.status == MasteryStatus.NEW -> Color(0xFF73758A)
        else -> PrimaryPurple
    }, label = "skillColor")
    val progress by animateFloatAsState(m.score / 100f, label = "mastery")
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(64.dp), color = color, strokeWidth = 5.dp)
                Text(if (m.status == MasteryStatus.NEW) "—" else "${m.score}", fontWeight = FontWeight.Bold, color = color)
            }
            Text(m.skill.name, fontWeight = FontWeight.Bold)
            Text(m.status.label, color = color, style = MaterialTheme.typography.labelMedium)
            if (m.needsFocus) Text("Cần ưu tiên", color = color, fontWeight = FontWeight.Bold)
            Text("${m.distinctQuestions}/${m.availableQuestions} câu đã luyện", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PracticeContent(state: LearningUiState, vm: AdaptivePracticeViewModel, busy: Boolean) {
    val session = state.session ?: return
    val question = state.bank.questions.find { it.id == session.currentQuestionId }
    val attempts = state.attempts.filter { it.sessionId == session.id }
    var selected by rememberSaveable(session.id, session.currentQuestionId) { mutableStateOf<Int?>(null) }
    var confidence by rememberSaveable(session.id, session.currentQuestionId) { mutableIntStateOf(2) }
    val stale = question == null || question.contentVersion != session.currentVersion
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("${attempts.count { !it.skipped }}/${session.targetCount} câu đã kiểm tra • ${attempts.count { it.skipped }} đã bỏ qua")
            LinearProgressIndicator(progress = { attempts.count { !it.skipped }.toFloat() / session.targetCount }, modifier = Modifier.fillMaxWidth())
        }
        item { LearningCard { Text("Vì sao chọn câu này?", fontWeight = FontWeight.Bold); Text(session.reason) } }
        if (stale) item {
            LearningCard {
                Text("Câu hỏi đã thay đổi hoặc bị xóa. Chọn câu khác để tiếp tục.")
                Button(onClick = { vm.next(skip = true) }, enabled = !busy) { Text("Chọn câu khác") }
            }
        } else {
            item {
                Text("${state.bank.skills.find { it.id == question.skillId }?.name ?: ""} • ${difficultyName(question.difficulty)}", color = PrimaryPurple)
                Text(question.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            val indices = question.options.indices.toList().shuffled(Random((session.id xor question.id).toInt()))
            items(indices) { index ->
                val chosen = if (session.answered) session.selectedOption else selected
                val correct = session.answered && index == question.correctOptionIndex
                val wrong = session.answered && chosen == index && !correct
                val container = when { correct -> Color(0xFFD8F4E5); wrong -> Color(0xFFFFDEDC)
                    chosen == index -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant }
                Card(Modifier.fillMaxWidth().clickable(enabled = !session.answered && !busy) { selected = index },
                    colors = CardDefaults.cardColors(containerColor = container)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen == index, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(question.options[index], Modifier.weight(1f))
                        if (correct) Icon(Icons.Default.CheckCircle, "Đáp án đúng", tint = Color(0xFF218C65))
                    }
                }
            }
            if (!session.answered) item {
                Text("Bạn tự tin đến đâu?")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Chưa chắc", 2 to "Khá chắc", 3 to "Rất chắc").forEach { (value, label) ->
                        FilterChip(selected = confidence == value, onClick = { confidence = value }, enabled = !busy, label = { Text(label) })
                    }
                }
                Button(onClick = { selected?.let { vm.answer(it, confidence) } }, enabled = selected != null && !busy, modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra") }
                TextButton(onClick = { vm.next(skip = true) }, enabled = !busy) { Text("Bỏ qua • không tính sai") }
            } else item {
                val attempt = attempts.find { it.questionId == question.id }
                LearningCard {
                    Text(if (session.selectedOption == question.correctOptionIndex) "Chính xác!" else "Cần luyện thêm câu này", fontWeight = FontWeight.Bold)
                    Text("Đáp án: ${question.options[question.correctOptionIndex]}")
                    attempt?.let { Text("Thời gian hoạt động: ${it.activeMillis / 1000}s • Đã lưu kết quả") }
                    Button(onClick = { vm.next() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (attempts.count { !it.skipped } >= session.targetCount) "Xem kết quả" else "Câu tiếp theo")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SessionSummary(state: LearningUiState) {
    val session = state.session ?: return
    val attempts = state.attempts.filter { it.sessionId == session.id }
    val answered = attempts.filter { !it.skipped }
    val scope = state.bank.questions.filter { q -> (session.quizId == null || q.quizId == session.quizId) &&
        (session.skillId == null || q.skillId == session.skillId) &&
        (session.folderId == null || state.bank.quizzes.find { it.id == q.quizId }?.folderId == session.folderId) }
    val mastery = MasteryCalculator.calculate(state.bank.skills, scope, state.attempts)
    LearningCard {
        Text("Kết quả phiên gần nhất", fontWeight = FontWeight.Bold)
        Text("${answered.count { it.correct }}/${answered.size} câu đúng • ${attempts.count { it.skipped }} bỏ qua")
        if (answered.size < session.targetCount) Text("Phiên kết thúc sớm hoặc đã hết câu phù hợp. Không lặp lại câu trong cùng phiên.")
        mastery.filter { m -> answered.any { it.skillId == m.skill.id } }.forEach { m ->
            val before = session.baseline[m.skill.id] ?: 0
            Text("${m.skill.name}: $before → ${m.score}/100 (${if (m.score - before >= 0) "+" else ""}${m.score - before})")
        }
    }
}

@Composable
private fun ClassificationContent(questions: List<QuestionEntity>, skills: List<SkillEntity>, busy: Boolean,
    onSave: (List<Long>, String, String, Int) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableIntStateOf(2) }
    var onlyUnclassified by rememberSaveable { mutableStateOf(true) }
    val visible = questions.filter { !onlyUnclassified || it.skillId == null }
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                LearningCard {
                    Text("Gắn một kỹ năng chính cho mỗi câu", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Môn / nhóm kiến thức") }, singleLine = true, enabled = !busy)
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tên kỹ năng") }, singleLine = true, enabled = !busy)
                    if (skills.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        skills.forEach { skill -> SuggestionChip(onClick = { subject = skill.subject; name = skill.name }, enabled = !busy,
                            label = { Text("${skill.subject} / ${skill.name}") }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { n -> FilterChip(selected = level == n, onClick = { level = n }, enabled = !busy, label = { Text(difficultyName(n)) }) }
                    }
                    Text("Lưu phân loại sẽ bắt đầu đánh giá lại các câu được chọn.", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyUnclassified, onCheckedChange = { onlyUnclassified = it; selected = emptyList() }, enabled = !busy)
                    Text("Chỉ câu chưa phân loại")
                }
                TextButton(onClick = { selected = if (visible.all { it.id in selected }) emptyList() else visible.map { it.id } }, enabled = !busy) {
                    Text("Chọn / bỏ chọn tất cả (${selected.size})")
                }
                if (visible.isEmpty()) Text("Không có câu chưa phân loại. Tắt bộ lọc để sửa nhãn câu đã có.")
            }
            items(visible, key = { it.id }) { q ->
                Card(Modifier.fillMaxWidth().clickable(enabled = !busy) { selected = if (q.id in selected) selected - q.id else selected + q.id }) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = q.id in selected, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.text)
                            Text("${skills.find { it.id == q.skillId }?.name ?: "Chưa phân loại"} • ${difficultyName(q.difficulty)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Button(onClick = { onSave(selected, subject, name, level) },
            enabled = !busy && selected.isNotEmpty() && subject.isNotBlank() && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(if (busy) "Đang lưu…" else "Lưu phân loại (${selected.size} câu)") }
    }
}
