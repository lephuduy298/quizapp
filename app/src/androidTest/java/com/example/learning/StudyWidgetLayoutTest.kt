package com.example.learning

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.runComposition
import androidx.glance.appwidget.provideContent
import androidx.glance.ExperimentalGlanceApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.widget.FocusStudyWidget
import com.example.widget.StudyProgressWidget
import com.example.widget.StudyWidgetState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudyWidgetLayoutTest {
    @OptIn(ExperimentalGlanceApi::class)
    @Test fun rendersAllSizesAndThemesWithVisibleReviewAction() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        for (focus in listOf(false, true)) {
        for (dark in listOf(false, true)) {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            }
            val context = app.createConfigurationContext(config)
            for ((width, height) in listOf(140 to 140, 180 to 220, 280 to 140, 320 to 220)) {
                val widget = object : GlanceAppWidget() {
                    override suspend fun provideGlance(context: Context, id: GlanceId) {
                        provideContent {
                            (if (focus) FocusStudyWidget() else StudyProgressWidget()).Content(StudyWidgetState(
                                user = "QuizAI", streak = 7, due = 12, questions = 40, activeToday = true))
                        }
                    }
                }
                val remote = widget.runComposition(context, sizes = listOf(DpSize(width.dp, height.dp))).first()
                val density = context.resources.displayMetrics.density
                val w = (width * density).toInt()
                val h = (height * density).toInt()
                val view = remote.apply(context, FrameLayout(context))
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, w, h)
                fun textViews(v: View): List<TextView> = when (v) {
                    is TextView -> listOf(v)
                    is ViewGroup -> (0 until v.childCount).flatMap { textViews(v.getChildAt(it)) }
                    else -> emptyList()
                }
                val button = textViews(view).single { it.text.toString() == "Ôn ngay" }
                val rect = android.graphics.Rect()
                assertTrue("Review action visible at ${width}x$height", button.getGlobalVisibleRect(rect))
                assertTrue("Review action fits widget", rect.bottom <= h)
                assertTrue("Review label is not clipped", rect.height() == button.height)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                File(app.getExternalFilesDir(null), "${if (focus) "focus" else "widget"}-${width}x$height-${if (dark) "dark" else "light"}.png")
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        }
    }
}
