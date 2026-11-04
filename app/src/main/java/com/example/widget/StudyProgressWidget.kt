package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.ActionParameters
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.QuizApplication
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudyWidgetState(
    val user: String? = null,
    val streak: Int = 0,
    val due: Int = 0,
    val questions: Int = 0,
    val activeToday: Boolean = false,
    val failed: Boolean = false
)

open class StudyProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(
        DpSize(140.dp, 140.dp), DpSize(140.dp, 220.dp),
        DpSize(280.dp, 140.dp), DpSize(280.dp, 220.dp)
    ))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) { loadState(context) }
        provideContent {
            var current by remember { mutableStateOf(state) }
            val revision by StudyWidgetUpdates.revision.collectAsState()
            LaunchedEffect(revision) {
                current = withContext(Dispatchers.IO) { loadState(context) }
            }
            Content(current)
        }
    }

    private suspend fun loadState(context: Context): StudyWidgetState {
        val container = (context.applicationContext as QuizApplication).container
        val manager = container.userManager
        // A login/logout can happen while Room is loading. Never publish that old account's snapshot.
        repeat(3) {
            val user = manager.getCurrentUser() ?: return StudyWidgetState()
            val streak = manager.getActiveStreakCount()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val activeToday = today in manager.getActiveDates()
            val result = try {
                val summary = container.reviewRepository.summary(user)
                StudyWidgetState(user, streak, summary.dueCount, summary.questionCount, activeToday)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { StudyWidgetState(user = user, failed = true) }
            if (manager.getCurrentUser() == user) return result
        }
        return StudyWidgetState(failed = true)
    }

    @Composable
    internal open fun Content(state: StudyWidgetState) {
        val wide = LocalSize.current.width >= 280.dp
        val tall = LocalSize.current.height >= 220.dp
        val ink = ColorProvider(R.color.study_widget_ink)
        val muted = ColorProvider(R.color.study_widget_muted)
        val accent = ColorProvider(R.color.study_widget_accent)
        val open = Intent(LocalContext.current, MainActivity::class.java).apply {
            action = MainActivity.ACTION_WIDGET_REVIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        Column(GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.study_widget_background)).appWidgetBackground()
            .cornerRadius(28.dp).padding(if (tall) 12.dp else if (wide) 16.dp else 10.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text("QuizAI", GlanceModifier.defaultWeight(), style = TextStyle(color = accent,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Box(GlanceModifier.size(if (tall || wide) 28.dp else 24.dp)
                    .background(ImageProvider(R.drawable.study_widget_surface)).cornerRadius(14.dp)
                    .clickable(actionRunCallback<RefreshStudyWidget>()), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(R.drawable.study_widget_refresh), "Làm mới tiến độ",
                        GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(muted))
                }
            }
            if (tall) Spacer(GlanceModifier.defaultWeight())
            if (state.failed || state.user == null) {
                if (tall) {
                    Image(ImageProvider(R.drawable.study_widget_book), null, GlanceModifier.size(36.dp),
                        colorFilter = ColorFilter.tint(accent))
                    Spacer(GlanceModifier.height(8.dp))
                }
                Text(if (state.failed) "Không thể tải dữ liệu" else "Đăng nhập để xem tiến độ",
                    style = TextStyle(color = ink, fontSize = if (tall) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold), maxLines = 2)
                if (tall || wide) Text(if (state.failed) "Bấm làm mới để thử lại" else "Một chút mỗi ngày, tiến bộ mỗi ngày.",
                    style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 2)
            } else {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Box(GlanceModifier.size(if (tall) 52.dp else 32.dp)
                        .background(ImageProvider(R.drawable.study_widget_flame_tile)).cornerRadius(16.dp),
                        contentAlignment = Alignment.Center) {
                        Image(ImageProvider(R.drawable.study_widget_flame), null,
                            GlanceModifier.size(if (tall) 34.dp else 26.dp))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(state.streak.toString(), style = TextStyle(color = ink,
                            fontSize = if (tall) 34.sp else 22.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                        Text("ngày liên tiếp", style = TextStyle(color = muted,
                            fontSize = 11.sp), maxLines = 1)
                    }
                    if (wide) {
                        Spacer(GlanceModifier.width(12.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            ReviewStatus(state, tall)
                            Text(state.user, style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
                if (!wide) {
                    if (tall) Spacer(GlanceModifier.height(8.dp))
                    ReviewStatus(state, tall)
                }
                if (tall && wide) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text(if (state.activeToday) "Hôm nay đã hoạt động" else "Học một chút, giữ nhịp mỗi ngày",
                        style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 2)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (tall) Spacer(GlanceModifier.height(6.dp))
            Box(GlanceModifier.fillMaxWidth().height(if (tall) 36.dp else 30.dp)
                .background(ImageProvider(R.drawable.study_widget_button)).cornerRadius(16.dp)
                .clickable(actionStartActivity(open)), contentAlignment = Alignment.Center) {
                Text(if (state.user == null && !state.failed) "Đăng nhập" else "Ôn ngay",
                    style = TextStyle(color = ColorProvider(R.color.study_widget_on_accent),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }

    @Composable
    private fun ReviewStatus(state: StudyWidgetState, expanded: Boolean) {
        Column(if (expanded) GlanceModifier.fillMaxWidth()
            .background(ImageProvider(R.drawable.study_widget_surface)).cornerRadius(14.dp).padding(8.dp)
            else GlanceModifier) {
            Text(when {
                state.questions == 0 -> "Chưa có câu hỏi"
                else -> "${state.due} câu đến hạn"
            }, style = TextStyle(color = ColorProvider(R.color.study_widget_ink),
                fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            if (expanded) Text(when {
                state.questions == 0 -> "Thêm câu hỏi mới"
                state.due == 0 -> "Đã bắt kịp lịch ôn"
                else -> "Sẵn sàng để ôn tập"
            }, style = TextStyle(color = ColorProvider(R.color.study_widget_muted), fontSize = 11.sp), maxLines = 2)
        }
    }

}

class StudyProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyProgressWidget()
}

class RefreshStudyWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        StudyWidgetUpdates.requestRefresh()
        StudyProgressWidget().updateAll(context)
        FocusStudyWidget().updateAll(context)
    }
}



