package com.example.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R

/** An independently selectable widget that shares the live account/review snapshot. */
class FocusStudyWidget : StudyProgressWidget() {
    @Composable
    internal override fun Content(state: StudyWidgetState) {
        val tall = LocalSize.current.height >= 220.dp
        val wide = LocalSize.current.width >= 280.dp
        val ink = ColorProvider(R.color.focus_widget_ink)
        val muted = ColorProvider(R.color.focus_widget_muted)
        val open = Intent(LocalContext.current, MainActivity::class.java).apply {
            action = MainActivity.ACTION_WIDGET_REVIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        Column(GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(ImageProvider(R.drawable.focus_widget_background))
            .padding(if (tall) 14.dp else 10.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text("QuizAI", GlanceModifier.defaultWeight(),
                    style = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Box(GlanceModifier.size(24.dp).clickable(actionRunCallback<RefreshStudyWidget>()),
                    contentAlignment = Alignment.Center) {
                    Image(ImageProvider(R.drawable.study_widget_refresh), "Làm mới tiến độ",
                        GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(muted))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (state.failed || state.user == null) {
                Text(if (state.failed) "Không thể tải dữ liệu" else "Đăng nhập để xem tiến độ",
                    style = TextStyle(color = ink, fontSize = if (tall) 20.sp else 14.sp,
                        fontWeight = FontWeight.Bold), maxLines = 2)
                if (tall) Text(if (state.failed) "Bấm làm mới để thử lại" else "Dành một chút thời gian cho kiến thức.",
                    style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 2)
            } else {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(if (state.questions == 0) "Bắt đầu nhé" else state.due.toString(),
                            style = TextStyle(color = ink, fontSize = if (state.questions == 0) 20.sp
                                else if (tall) 36.sp else 24.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                        Text(if (state.questions == 0) "Thêm câu hỏi mới" else "câu đến hạn",
                            style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
                    }
                    if (wide) {
                        Spacer(GlanceModifier.width(16.dp))
                        Column {
                            Streak(state)
                            Text(state.user, style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
                if (tall && !wide) {
                    Spacer(GlanceModifier.height(12.dp))
                    Streak(state)
                }
                if (tall && wide && state.questions > 0) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text(if (state.due == 0) "Đã bắt kịp lịch ôn. Giỏi lắm!" else "Một lượt ôn, nhớ lâu hơn.",
                        style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.height(6.dp))
            Box(GlanceModifier.fillMaxWidth().height(if (tall) 36.dp else 30.dp)
                .background(ImageProvider(R.drawable.focus_widget_button)).clickable(actionStartActivity(open)),
                contentAlignment = Alignment.Center) {
                Text(if (state.user == null && !state.failed) "Đăng nhập" else "Ôn ngay",
                    style = TextStyle(color = ColorProvider(R.color.focus_widget_on_accent),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }

    @Composable
    private fun Streak(state: StudyWidgetState) {
        Row(GlanceModifier.background(ImageProvider(R.drawable.focus_widget_chip)).padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(ImageProvider(R.drawable.study_widget_flame), null, GlanceModifier.size(18.dp))
            Spacer(GlanceModifier.width(4.dp))
            Text("${state.streak} ngày liên tiếp", style = TextStyle(color = ColorProvider(R.color.focus_widget_ink),
                fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }
}

class FocusStudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusStudyWidget()
}
