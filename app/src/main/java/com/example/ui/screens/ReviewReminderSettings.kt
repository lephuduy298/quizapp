package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.UserManager
import com.example.reminder.ReviewDueReminder

@Composable
fun ReviewReminderSettings() {
    val context = LocalContext.current
    val manager = remember(context) { UserManager.getInstance(context) }
    val user = manager.getCurrentUser()
    var enabled by remember(user) { mutableStateOf(manager.isReviewReminderEnabled()) }
    var allowed by remember(user) { mutableStateOf(ReviewDueReminder.canNotify(context)) }
    var requestedUser by rememberSaveable { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (requestedUser != null && manager.getCurrentUser() == requestedUser) {
            manager.setReviewReminderEnabled(granted)
            enabled = granted
            allowed = ReviewDueReminder.canNotify(context)
            ReviewDueReminder.refresh(context)
            if (!granted) Toast.makeText(context,
                "Hãy cho phép thông báo để nhận nhắc khi đến lịch ôn.", Toast.LENGTH_LONG).show()
        }
        requestedUser = null
    }
    DisposableEffect(owner, user) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = manager.isReviewReminderEnabled()
                allowed = ReviewDueReminder.canNotify(context)
                ReviewDueReminder.refresh(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nhắc khi đến lịch ôn", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Switch(checked = enabled, enabled = user != null,
                    modifier = Modifier.testTag("review_reminder_toggle")
                        .semantics { contentDescription = "Nhắc khi đến lịch ôn" },
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestedUser = user
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            manager.setReviewReminderEnabled(checked)
                            enabled = checked
                            allowed = ReviewDueReminder.canNotify(context)
                            ReviewDueReminder.refresh(context)
                        }
                    })
            }
            Text("Gửi thông báo khi có câu đến hạn. Chạm thông báo để ôn ngay.",
                style = MaterialTheme.typography.bodySmall)
            if (!allowed) {
                Text("Cho phép thông báo để nhận nhắc ôn luyện.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    context.startActivity(if (Build.VERSION.SDK_INT >= 26) {
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, ReviewDueReminder.CHANNEL_ID)
                    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}")))
                }) { Text("Cài đặt thông báo") }
            }
        }
    }
}
