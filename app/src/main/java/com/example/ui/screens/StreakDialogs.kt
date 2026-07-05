package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.QuizViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Custom Hexagon Shape for the Calendar and Celebration Row
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun StreakDetailsDialog(
    viewModel: QuizViewModel,
    onDismiss: () -> Unit
) {
    val activeDates by viewModel.activeDates.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    val freezes by viewModel.streakFreezes.collectAsState()
    val context = LocalContext.current

    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF3E0).copy(alpha = 0.6f), Color.White)
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = SecondaryTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Outlined giant streak count & encouraged title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val density = LocalDensity.current
                        Text(
                            text = streakCount.toString(),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(
                                drawStyle = Stroke(
                                    width = with(density) { 4.dp.toPx() },
                                    join = StrokeJoin.Round
                                )
                            ),
                            color = Color(0xFFFF5722)
                        )
                        Text(
                            text = "Ngày Streak",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tuyệt vời! Rèn luyện thói quen học tập mỗi ngày!",
                            fontSize = 13.sp,
                            color = SecondaryTextColor,
                            lineHeight = 18.sp
                        )
                    }

                    // Pulsing Flame Logo
                    val infiniteTransition = rememberInfiniteTransition(label = "DetailsFlamePulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
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
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // CALENDAR HEADER & NAVIGATION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentCalendar = (currentCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Tháng trước",
                            tint = PrimaryPurple
                        )
                    }

                    val monthNames = listOf(
                        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
                    )
                    val monthStr = "${monthNames[currentCalendar.get(Calendar.MONTH)]} ${currentCalendar.get(Calendar.YEAR)}"
                    Text(
                        text = monthStr.uppercase(Locale.getDefault()),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BodyTextColor,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = {
                            currentCalendar = (currentCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Tháng sau",
                            tint = PrimaryPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekday headers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
                    days.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTextColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hexagonal calendar grid setup
                val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayCal = (currentCalendar.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
                val prefixEmptyCells = firstDayOfWeek - 1
                val totalCells = prefixEmptyCells + daysInMonth

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())

                val cellIndices = 0 until totalCells
                val chunkedWeeks = cellIndices.chunked(7)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedWeeks.forEach { weekIndices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (cellIndex in weekIndices) {
                                val dayOfMonth = cellIndex - prefixEmptyCells + 1
                                val isDayInMonth = cellIndex >= prefixEmptyCells

                                if (isDayInMonth) {
                                    val cellCal = (currentCalendar.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    val dateStr = sdf.format(cellCal.time)
                                    val isActive = activeDates.contains(dateStr)
                                    
                                    val dayOfWeek = cellCal.get(Calendar.DAY_OF_WEEK)
                                    
                                    val isPreviousActive = run {
                                        if (dayOfMonth > 1 && dayOfWeek != Calendar.SUNDAY) {
                                            val prevCal = (currentCalendar.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth - 1)
                                            }
                                            activeDates.contains(sdf.format(prevCal.time))
                                        } else false
                                    }
                                    val isNextActive = run {
                                        if (dayOfMonth < daysInMonth && dayOfWeek != Calendar.SATURDAY) {
                                            val nextCal = (currentCalendar.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth + 1)
                                            }
                                            activeDates.contains(sdf.format(nextCal.time))
                                        } else false
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Draw orange horizontal connection pill in background
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val cy = h / 2f
                                            if (isActive) {
                                                if (isPreviousActive) {
                                                    drawRect(
                                                        color = Color(0xFFFF9100),
                                                        topLeft = Offset(0f, cy - 8.dp.toPx()),
                                                        size = Size(w / 2f, 16.dp.toPx())
                                                    )
                                                }
                                                if (isNextActive) {
                                                    drawRect(
                                                        color = Color(0xFFFF9100),
                                                        topLeft = Offset(w / 2f, cy - 8.dp.toPx()),
                                                        size = Size(w / 2f, 16.dp.toPx())
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = dayOfMonth.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color(0xFFFF5722) else SecondaryTextColor.copy(alpha = 0.5f)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(2.dp))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .aspectRatio(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isActive) {
                                                    // Solid orange gradient hexagon
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val w = size.width
                                                        val h = size.height
                                                        val path = Path().apply {
                                                            moveTo(w * 0.5f, 0f)
                                                            lineTo(w, h * 0.25f)
                                                            lineTo(w, h * 0.75f)
                                                            lineTo(w * 0.5f, h)
                                                            lineTo(0f, h * 0.75f)
                                                            lineTo(0f, h * 0.25f)
                                                            close()
                                                        }
                                                        drawPath(
                                                            path = path,
                                                            brush = Brush.verticalGradient(
                                                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                                            )
                                                        )
                                                    }

                                                    if (dateStr == todayStr) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocalFireDepartment,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.fillMaxSize(0.6f)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.fillMaxSize(0.6f)
                                                        )
                                                    }
                                                } else {
                                                    // Inactive dashed gray hexagon
                                                    Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                                        val w = size.width
                                                        val h = size.height
                                                        val path = Path().apply {
                                                            moveTo(w * 0.5f, 0f)
                                                            lineTo(w, h * 0.25f)
                                                            lineTo(w, h * 0.75f)
                                                            lineTo(w * 0.5f, h)
                                                            lineTo(0f, h * 0.75f)
                                                            lineTo(0f, h * 0.25f)
                                                            close()
                                                        }
                                                        drawPath(
                                                            path = path,
                                                            color = BorderColor.copy(alpha = 0.5f),
                                                            style = Stroke(
                                                                width = 1.5.dp.toPx(),
                                                                pathEffect = PathEffect.dashPathEffect(
                                                                    floatArrayOf(8f, 8f), 0f
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                            if (weekIndices.size < 7) {
                                for (i in 0 until (7 - weekIndices.size)) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // STREAK FREEZES SECTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HighDensityBackground)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Đóng Băng Chuỗi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BodyTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Bảo vệ chuỗi khi lỡ quên học bài.",
                            fontSize = 11.sp,
                            color = SecondaryTextColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 1..2) {
                            val isActiveFreeze = freezes >= i
                            
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .graphicsLayer { rotationZ = if (i == 1) -8f else 12f }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isActiveFreeze) {
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFF80DEEA), Color(0xFF00B0FF))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(BorderColor.copy(alpha = 0.4f), BorderColor)
                                            )
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isActiveFreeze) Color(0xFFE0F7FA) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = if (isActiveFreeze) Color.White else SecondaryTextColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (freezes < 2) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = {
                                    viewModel.refillStreakFreezes()
                                    Toast.makeText(context, "Đã khôi phục 2 lượt đóng băng chuỗi! ❄️", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Khôi phục", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakCelebrationDialog(
    viewModel: QuizViewModel,
    streakCount: Int,
    onDismiss: () -> Unit
) {
    val activeDates by viewModel.activeDates.collectAsState()
    val context = LocalContext.current
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())

    // 5-day rolling window: yesterday, today, tomorrow, and 2 days after
    val rollingDays = remember {
        val days = mutableListOf<Pair<String, String>>()
        val enLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1) // yesterday
        for (i in 0 until 5) {
            val dateStr = sdf.format(calendar.time)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val label = enLabels[dayOfWeek - 1]
            days.add(dateStr to label)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Giant outlined flame with streak number overlay
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        val flamePath = Path().apply {
                            moveTo(w * 0.5f, h * 0.05f)
                            cubicTo(w * 0.7f, h * 0.2f, w * 0.95f, h * 0.42f, w * 0.9f, h * 0.72f)
                            cubicTo(w * 0.85f, h * 0.92f, w * 0.7f, h * 0.98f, w * 0.5f, h * 0.98f)
                            cubicTo(w * 0.3f, h * 0.98f, w * 0.15f, h * 0.92f, w * 0.1f, h * 0.72f)
                            cubicTo(w * 0.05f, h * 0.42f, w * 0.3f, h * 0.2f, w * 0.5f, h * 0.05f)
                            close()
                        }
                        
                        drawPath(
                            path = flamePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                            )
                        )
                        
                        val innerFlamePath = Path().apply {
                            moveTo(w * 0.5f, h * 0.35f)
                            cubicTo(w * 0.62f, h * 0.45f, w * 0.78f, h * 0.62f, w * 0.75f, h * 0.82f)
                            cubicTo(w * 0.7f, h * 0.93f, w * 0.62f, h * 0.95f, w * 0.5f, h * 0.95f)
                            cubicTo(w * 0.38f, h * 0.95f, w * 0.3f, h * 0.93f, w * 0.25f, h * 0.82f)
                            cubicTo(w * 0.22f, h * 0.62f, w * 0.38f, h * 0.45f, w * 0.5f, h * 0.35f)
                            close()
                        }
                        drawPath(
                            path = innerFlamePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFFEA00))
                            )
                        )
                    }

                    val density = LocalDensity.current
                    Text(
                        text = streakCount.toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            drawStyle = Stroke(
                                width = with(density) { 5.dp.toPx() },
                                join = StrokeJoin.Round
                            )
                        ),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center).padding(top = 18.dp)
                    )
                }

                Text(
                    text = "Day Streak",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5722)
                )

                // 5-day rolling weekly progress row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rollingDays.forEach { (dateStr, label) ->
                        val isActive = activeDates.contains(dateStr)
                        val isTodayCell = dateStr == todayStr

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color(0xFFFF9100) else SecondaryTextColor.copy(alpha = 0.4f)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val path = Path().apply {
                                            moveTo(w * 0.5f, 0f)
                                            lineTo(w, h * 0.25f)
                                            lineTo(w, h * 0.75f)
                                            lineTo(w * 0.5f, h)
                                            lineTo(0f, h * 0.75f)
                                            lineTo(0f, h * 0.25f)
                                            close()
                                        }
                                        drawPath(
                                            path = path,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                            )
                                        )
                                    }

                                    if (isTodayCell) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize(0.6f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize(0.6f)
                                        )
                                    }
                                } else {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        val path = Path().apply {
                                            moveTo(w * 0.5f, 0f)
                                            lineTo(w, h * 0.25f)
                                            lineTo(w, h * 0.75f)
                                            lineTo(w * 0.5f, h)
                                            lineTo(0f, h * 0.75f)
                                            lineTo(0f, h * 0.25f)
                                            close()
                                        }
                                        drawPath(
                                            path = path,
                                            color = BorderColor.copy(alpha = 0.5f),
                                            style = Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(8f, 8f), 0f
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Thật tuyệt khi giữ vững thói quen học tập hôm nay!",
                    fontSize = 14.sp,
                    color = SecondaryTextColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Tiếp tục",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Đang chuẩn bị chia sẻ chuỗi ngày học của bạn...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = "Chia sẻ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00B0FF)
                        )
                    }
                }
            }
        }
    }
}
