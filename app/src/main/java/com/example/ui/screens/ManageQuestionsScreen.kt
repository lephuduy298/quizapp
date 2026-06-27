package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.QuizWithQuestions
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.QuestionEntity
import com.example.ui.QuizGenerationState
import com.example.ui.QuizViewModel
import com.example.ui.theme.*

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
    val generationState by viewModel.generationState.collectAsState()
    
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showAIChatDialog by remember { mutableStateOf(false) }
    var showAICreationSelection by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var questionToDelete by remember { mutableStateOf<QuestionEntity?>(null) }

    LaunchedEffect(generationState) {
        if (generationState is QuizGenerationState.Success) {
            viewModel.resetGenerationState()
            showAIChatDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quizWithQuestions?.quiz?.title ?: "Quản lý câu hỏi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (quizWithQuestions != null && quizWithQuestions.questions.isNotEmpty()) {
                        val context = LocalContext.current
                        IconButton(onClick = { exportQuizToPdf(context, quizWithQuestions) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Xuất PDF và Chia sẻ", tint = PrimaryPurple)
                        }
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
                // Add via AI Button
                FloatingActionButton(
                    onClick = { showAICreationSelection = true },
                    containerColor = Color(0xFF03A9F4),
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Thêm bằng AI")
                }

                // Add Manually Button
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                .fillMaxWidth()
                                .clickable {
                                    editingQuestion = question
                                    showAddManualDialog = true
                                },
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

            // PROCESSING OVERLAY
            if (generationState !is QuizGenerationState.Idle) {
                AIGenerationOverlay(
                    state = generationState,
                    onReset = { viewModel.resetGenerationState() }
                )
            }
        }
    }

    if (showAddManualDialog) {
        AddEditQuestionDialog(
            editingQuestion = editingQuestion,
            onDismiss = {
                showAddManualDialog = false
                editingQuestion = null
            },
            onSave = { questionText, options, correctIndex ->
                if (editingQuestion == null) {
                    viewModel.addQuestion(quizId, questionText, options, correctIndex)
                } else {
                    viewModel.updateQuestion(editingQuestion!!.id, quizId, questionText, options, correctIndex)
                }
                showAddManualDialog = false
                editingQuestion = null
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

    if (showAIChatDialog) {
        AIChatGenerationDialog(
            onDismiss = { showAIChatDialog = false },
            onGenerate = { prompt ->
                viewModel.generateQuizFromText(prompt, quizId)
                showAIChatDialog = false
            }
        )
    }

    if (showAICreationSelection) {
        AICreationSelectionDialog(
            onDismiss = { showAICreationSelection = false },
            onSelectChat = {
                showAICreationSelection = false
                showAIChatDialog = true
            },
            onSelectCamera = {
                showAICreationSelection = false
                onAddViaAI(quizId)
            },
            title = "Thêm câu hỏi bằng AI"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditQuestionDialog(
    editingQuestion: QuestionEntity?,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, Int) -> Unit
) {
    var questionText by remember { mutableStateOf(editingQuestion?.text ?: "") }
    val initialOptions = editingQuestion?.options ?: listOf("", "", "", "")

    var option0 by remember { mutableStateOf(initialOptions.getOrNull(0) ?: "") }
    var option1 by remember { mutableStateOf(initialOptions.getOrNull(1) ?: "") }
    var option2 by remember { mutableStateOf(initialOptions.getOrNull(2) ?: "") }
    var option3 by remember { mutableStateOf(initialOptions.getOrNull(3) ?: "") }

    var correctIndex by remember { mutableIntStateOf(editingQuestion?.correctOptionIndex ?: 0) }

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
                    .padding(24.dp)
            ) {
                Text(
                    text = if (editingQuestion == null) "Thêm câu hỏi thủ công" else "Sửa câu hỏi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BodyTextColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Câu hỏi") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )

                    val optionsState = listOf(
                        Triple(option0, { it: String -> option0 = it }, "A"),
                        Triple(option1, { it: String -> option1 = it }, "B"),
                        Triple(option2, { it: String -> option2 = it }, "C"),
                        Triple(option3, { it: String -> option3 = it }, "D")
                    )

                    optionsState.forEachIndexed { index, (optValue, optChange, letter) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (correctIndex == index),
                                onClick = { correctIndex = index }
                            )
                            OutlinedTextField(
                                value = optValue,
                                onValueChange = optChange,
                                label = { Text("Đáp án $letter") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            val optionsStrs = listOf(option0, option1, option2, option3)
                            if (questionText.isNotBlank() && optionsStrs.all { it.isNotBlank() }) {
                                onSave(questionText, optionsStrs, correctIndex)
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

fun exportQuizToPdf(context: Context, quizWithQuestions: QuizWithQuestions) {
    val pdfDocument = PdfDocument()
    val paint = Paint()
    val titlePaint = Paint().apply {
        textSize = 16f
        isFakeBoldText = true
    }
    val bodyPaint = Paint().apply {
        textSize = 11f
    }
    val boldPaint = Paint().apply {
        textSize = 11f
        isFakeBoldText = true
    }

    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    var y = 50f
    val margin = 50f
    val contentWidth = pageWidth - (margin * 2)

    fun drawTextLine(text: String, currentPaint: Paint, indent: Float = 0f) {
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val textWidth = currentPaint.measureText(testLine)
            if (textWidth > (contentWidth - indent)) {
                canvas.drawText(line, margin + indent, y, currentPaint)
                y += currentPaint.textSize + 6f
                if (y > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin
                }
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, margin + indent, y, currentPaint)
            y += currentPaint.textSize + 6f
        }
    }

    try {
        drawTextLine(quizWithQuestions.quiz.title.uppercase(), titlePaint)
        y += 10f
        drawTextLine("Chủ đề: ${quizWithQuestions.quiz.topic}", boldPaint)
        drawTextLine("Số lượng câu hỏi: ${quizWithQuestions.questions.size} câu", bodyPaint)
        drawTextLine("Thời gian làm bài: ${quizWithQuestions.quiz.durationMinutes} phút", bodyPaint)
        y += 20f

        quizWithQuestions.questions.forEachIndexed { qIdx, question ->
            drawTextLine("Câu ${qIdx + 1}: ${question.text}", boldPaint)
            y += 4f
            question.options.forEachIndexed { oIdx, option ->
                val prefix = when (oIdx) {
                    0 -> "A. "
                    1 -> "B. "
                    2 -> "C. "
                    3 -> "D. "
                    else -> "${(oIdx + 65).toChar()}. "
                }
                drawTextLine("$prefix$option", bodyPaint, indent = 20f)
            }
            y += 12f
        }

        y += 10f
        drawTextLine("-----------------------------", bodyPaint)
        y += 5f
        drawTextLine("ĐÁP ÁN THAM KHẢO", boldPaint)
        y += 5f
        val answersList = quizWithQuestions.questions.mapIndexed { qIdx, question ->
            val correctLetter = when (question.correctOptionIndex) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> (question.correctOptionIndex + 65).toChar().toString()
            }
            "${qIdx + 1}-$correctLetter"
        }
        
        drawTextLine(answersList.joinToString(", "), bodyPaint)

        pdfDocument.finishPage(page)

        val pdfFile = File(context.cacheDir, "${quizWithQuestions.quiz.title.replace(" ", "_")}_quiz.pdf")
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Đề thi: ${quizWithQuestions.quiz.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ đề thi PDF qua..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi xuất PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        try {
            pdfDocument.close()
        } catch (ignored: Exception) {}
    }
}
