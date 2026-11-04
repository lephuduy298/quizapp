package com.example.learning

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.remote.GeneratedQuizJson
import com.example.data.repository.*
import com.example.ui.QuizViewModel
import com.example.ui.screens.FlashcardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlashcardSwipeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var manager: UserManager
    private val store = ViewModelStore()
    private val user = "flashcardswipe"

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        manager = UserManager.getInstance(context)
        manager.register(user, "testpassword")
        check(manager.login(user, "testpassword"))
        val quizId = runBlocking {
            db.quizDao().insertQuizWithQuestions(
                QuizEntity(title = "Bộ thẻ kiểm tra", topic = "Android", durationMinutes = 5),
                listOf("Câu hỏi thứ nhất", "Câu hỏi thứ hai").map { text ->
                    QuestionEntity(quizId = 0, text = text,
                        options = listOf("Đáp án đúng", "Đáp án sai"), correctOptionIndex = 0)
                })
        }
        val unusedAI = object : AIRepository {
            override suspend fun generateQuizFromImage(bitmap: Bitmap, promptAddition: String): Result<GeneratedQuizJson> = error("No AI calls expected")
            override suspend fun generateQuizFromText(prompt: String): Result<GeneratedQuizJson> = error("No AI calls expected")
            override suspend fun getExplanationForAnswer(questionText: String, options: List<String>, correctAnswer: String, userAnswer: String): Result<String> = error("No AI calls expected")
        }
        val vm = QuizViewModel(QuizRepository(db.quizDao(), db.quizSessionDao(), db.folderDao()),
            unusedAI, manager, ReviewRepository(db))
        store.put("quiz", vm)
        compose.setContent { MyApplicationTheme { FlashcardScreen(vm, quizId, onNavigateBack = {}) } }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("flashcard_card").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After fun close() {
        store.clear()
        manager.logout()
        db.close()
    }

    private fun progress() = runBlocking { db.reviewDao().allProgress(user) }

    private fun flip() {
        compose.onNodeWithTag("flashcard_card").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("ĐÁP ÁN ĐÚNG", substring = false).assertExists()
    }

    private fun swipe(right: Boolean, fraction: Float = 0.4f) {
        compose.onNodeWithTag("flashcard_swipe_area").performScrollTo().performTouchInput {
            swipe(start = center, end = center + Offset(width * fraction * if (right) 1f else -1f, 0f),
                durationMillis = 400)
        }
        compose.waitForIdle()
    }

    @Test fun frontFaceCanBeSwipedBothWaysWithoutRevealingAnswers() {
        assertSwipeRatings(revealAnswers = false)
    }

    @Test fun flippedFaceKeepsTheSameSwipeDirectionsAndResetsTheNextCard() {
        assertSwipeRatings(revealAnswers = true)
    }

    private fun assertSwipeRatings(revealAnswers: Boolean) {
        if (revealAnswers) flip()
        swipe(right = true)
        compose.waitUntil(15000) { progress().size == 1 }
        compose.onNodeWithText("Thẻ 2 trên 2").assertExists()
        compose.onNodeWithText("CÂU HỎI", substring = false).assertExists()
        compose.onNodeWithText("ĐÁP ÁN ĐÚNG", substring = false).assertDoesNotExist()
        val remembered = progress().single()
        assertEquals(1, remembered.stage)
        assertEquals(1, remembered.reviewCount)
        assertEquals(0, remembered.mistakeCount)
        assertTrue(remembered.nextReviewAt!! > System.currentTimeMillis() + 23 * 60 * 60_000L)

        if (revealAnswers) flip()
        swipe(right = false)
        compose.waitUntil(15000) { progress().size == 2 }
        compose.onNodeWithText("Hoàn Thành Phiên Học!").assertExists()
        val forgotten = progress().single { it.questionId != remembered.questionId }
        assertEquals(0, forgotten.stage)
        assertEquals(1, forgotten.reviewCount)
        assertEquals(0, forgotten.mistakeCount)
        assertTrue(forgotten.nextReviewAt!! in System.currentTimeMillis()..(System.currentTimeMillis() + 10 * 60_000L))
    }

    @Test fun shortDragAndCancelledDragDoNotRateOrAdvance() {
        swipe(right = false, fraction = 0.1f)
        assertTrue(progress().isEmpty())
        compose.onNodeWithText("Thẻ 1 trên 2").assertExists()

        compose.onNodeWithTag("flashcard_swipe_area").performTouchInput {
            down(center)
            moveTo(center + Offset(width * 0.4f, 0f), delayMillis = 400)
            cancel()
        }
        compose.waitForIdle()
        assertTrue(progress().isEmpty())
        compose.onNodeWithText("Thẻ 1 trên 2").assertExists()

        swipe(right = true)
        compose.waitUntil(15000) { progress().size == 1 }
        assertEquals(1, progress().single().reviewCount)
    }

    @Test fun flipAndSwipeFeedbackStayTogetherOnBothFaces() {
        listOf(false, true).forEach { back ->
            if (back) flip()
            compose.onNodeWithTag("flashcard_swipe_area").performScrollTo().performTouchInput {
                down(center)
                moveTo(center + Offset(-width * 0.2f, 0f), delayMillis = 400)
            }
            compose.onNodeWithText("CHƯA THUỘC", substring = false).assertExists()
            val working = File(requireNotNull(System.getProperty("user.dir")))
            val root = if (File(working, "settings.gradle.kts").exists()) working else working.parentFile
            compose.onRoot().captureRoboImage(filePath = File(root,
                ".build-outputs/flashcard-${if (back) "back" else "front"}-swipe.png").absolutePath)
            compose.onNodeWithTag("flashcard_swipe_area").performTouchInput { cancel() }
            compose.waitForIdle()
            assertTrue(progress().isEmpty())
        }
        // Tapping still performs the original 350ms flip back to the question.
        compose.onNodeWithTag("flashcard_card").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("CÂU HỎI", substring = false).assertExists()
        compose.onNodeWithText("ĐÁP ÁN ĐÚNG", substring = false).assertDoesNotExist()
    }
}
