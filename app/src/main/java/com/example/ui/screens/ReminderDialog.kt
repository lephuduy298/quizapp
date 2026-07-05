package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.QuizViewModel
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
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

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

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
