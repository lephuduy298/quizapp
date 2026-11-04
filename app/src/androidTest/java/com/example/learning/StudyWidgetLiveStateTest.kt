package com.example.learning

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.runComposition
import androidx.test.core.app.ApplicationProvider
import com.example.QuizApplication
import com.example.data.local.AppDatabase
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizEntity
import com.example.data.local.ReviewProgressEntity
import com.example.widget.FocusStudyWidget
import com.example.widget.StudyProgressWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import androidx.test.ext.junit.runners.AndroidJUnit4


@RunWith(AndroidJUnit4::class)

class StudyWidgetLiveStateTest {
    @Test fun sameCompositionUpdatesOnLoginAndClearsAccountOnLogout() = runBlocking {
        verifyLiveState(StudyProgressWidget(), false)
    }

    @Test fun focusWidgetUpdatesOnLoginReviewAndLogout() = runBlocking {
        verifyLiveState(FocusStudyWidget(), true)
    }

    @OptIn(ExperimentalGlanceApi::class)
    private suspend fun verifyLiveState(widget: StudyProgressWidget, focus: Boolean) = coroutineScope {
        val app = ApplicationProvider.getApplicationContext<QuizApplication>()
        val manager = app.container.userManager
        val user = "widgetlive_" + UUID.randomUUID().toString().take(8)
        val database = AppDatabase.getDatabase(app)
        var quizId: Long? = null
        manager.logout()
        val views = Channel<RemoteViews>(Channel.UNLIMITED)
        val job = launch(Dispatchers.Default) {
            widget.runComposition(app, sizes = listOf(DpSize(280.dp, 140.dp)))
                .collect { views.send(it) }
        }
        fun texts(view: View): List<String> = when (view) {
            is TextView -> listOf(view.text.toString())
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }
        suspend fun awaitText(expected: String): List<String> = withTimeout(20000) {
            var content: List<String>
            do { content = texts(views.receive().apply(app, FrameLayout(app))) }
            while (expected !in content)
            content
        }
        try {
            awaitText("Đăng nhập để xem tiến độ")
            assertTrue(manager.register(user, "testpassword"))
            assertTrue(manager.login(user, "testpassword"))
            manager.updateStreak()
            val signedIn = awaitText(user)
            assertTrue(if (focus) "1 ngày liên tiếp" in signedIn else "1" in signedIn && "ngày liên tiếp" in signedIn)
            assertTrue("Ôn ngay" in signedIn)
            quizId = database.quizDao().insertQuiz(QuizEntity(title = "Widget integration test", topic = "Test", durationMinutes = 1))
            val question = QuestionEntity(quizId = requireNotNull(quizId), text = "Test question",
                options = listOf("A", "B"), correctOptionIndex = 0)
            val questionId = database.quizDao().insertQuestion(question)
            database.reviewDao().save(ReviewProgressEntity(user, questionId, 1, nextReviewAt = 1))
            awaitText(if (focus) "câu đến hạn" else "1 câu đến hạn")
            assertTrue(app.container.reviewRepository.rate(user, questionId, 1, ReviewRating.GOOD, "widget-test"))
            awaitText(if (focus) "0" else "0 câu đến hạn")
            manager.logout()
            val signedOut = awaitText("Đăng nhập để xem tiến độ")
            assertFalse(user in signedOut)
            assertFalse(signedOut.any { it.contains("ngày liên tiếp") })
            assertTrue("Đăng nhập" in signedOut)
        } finally {
            job.cancelAndJoin()
            views.close()
            manager.logout()
            quizId?.let { database.quizDao().deleteQuizById(it) }
        }
    }
}


