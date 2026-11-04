package com.example.learning

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.LearningRepository
import com.example.data.repository.ReviewRepository
import com.example.ui.ReviewViewModel
import com.example.ui.screens.SpacedReviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpacedReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private var db: AppDatabase? = null
    private val store = ViewModelStore()
    @After fun close() { store.clear(); db?.close() }

    @Test fun notebookCanSaveAnnotationsAndRateOfflineWithoutLosingTheActiveCard() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        db = database
        val repo = ReviewRepository(database)
        val question = runBlocking {
            LearningRepository(database).installDemo()
            val q = database.learningDao().questions().first()
            repo.recordAnswer("student", q.id, 1, 1, "seed")
            q
        }
        val vm = ReviewViewModel(repo, "student", refreshIntervalMillis = 0)
        store.put("review", vm)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MyApplicationTheme { SpacedReviewScreen(vm, onBack = {}) } }
        compose.waitUntil(15000) { compose.waitForIdle(); vm.state.value.loaded }
        compose.onNodeWithText("Câu sai", useUnmergedTree = true).performClick()
        compose.onNodeWithText(question.text).performScrollTo().performClick()
        compose.onNodeWithTag("review_note").performTextInput("Ghi nhớ vòng đời Activity")
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Lưu ghi chú").performClick()
        compose.waitUntil(15000) {
            compose.waitForIdle()
            vm.state.value.entries.find { it.question.id == question.id }?.progress?.note == "Ghi nhớ vòng đời Activity"
        }
        assertNull(vm.error.value)
        compose.onNodeWithText("Ôn câu này").performScrollTo().assertExists()
        compose.onRoot().captureRoboImage(filePath = artifact("review-notebook.png"))
        compose.onNodeWithText("Ôn câu này").performClick()
        compose.onNodeWithText("Hiện đáp án").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Nhớ", substring = false).assertIsEnabled()
        compose.onRoot().captureRoboImage(filePath = artifact("review-card.png"))
        compose.onNodeWithText("Nhớ", substring = false).performClick()
        compose.waitUntil(15000) {
            compose.waitForIdle()
            vm.state.value.entries.find { it.question.id == question.id }?.progress?.reviewCount == 2
        }
        compose.onNodeWithText("Đã hoàn thành phiên ôn!").assertExists()
        val saved = runBlocking { requireNotNull(database.reviewDao().progress("student", question.id)) }
        assertEquals(1, saved.mistakeCount)
        assertTrue(saved.isFavorite)
        assertTrue(saved.nextReviewAt!! > System.currentTimeMillis())
        compose.onNodeWithText("Về danh sách").performClick()
        compose.onNodeWithText("Quan trọng", substring = false).performClick()
        compose.onNodeWithText(question.text).performScrollTo().assertExists()
    }

    @Test fun dueQueueAdvancesOnceWithoutBeingRebuiltWhenSchedulesChange() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        db = database
        val past = System.currentTimeMillis() - 601000
        val repo = ReviewRepository(database) { past }
        val questions = runBlocking {
            LearningRepository(database).installDemo()
            database.learningDao().questions().take(2).also { list ->
                list.forEach { repo.recordAnswer("student", it.id, 1, 1, "seed:${it.id}") }
            }
        }
        val vm = ReviewViewModel(repo, "student", refreshIntervalMillis = 0)
        store.put("review", vm)
        compose.setContent { MyApplicationTheme { SpacedReviewScreen(vm, onBack = {}) } }
        compose.waitUntil(15000) { compose.waitForIdle(); vm.state.value.loaded }
        compose.onRoot().captureRoboImage(filePath = artifact("review-due.png"))
        compose.onNodeWithText("Ôn danh sách này (2 thẻ)").performScrollTo().performClick()
        questions.forEach { q ->
            compose.onNodeWithText(q.text).assertExists()
            compose.onNodeWithText("Hiện đáp án").performScrollTo().performClick()
            compose.onNodeWithText("Nhớ", substring = false).performScrollTo().performClick()
            compose.waitUntil(15000) {
                compose.waitForIdle()
                vm.state.value.entries.find { it.question.id == q.id }?.progress?.reviewCount == 2
            }
        }
        compose.onNodeWithText("Đã hoàn thành phiên ôn!").assertExists()
        compose.onNodeWithText("Về danh sách").performClick()
        compose.onNodeWithText("Ôn danh sách này (0 thẻ)").performScrollTo().assertIsNotEnabled()
        assertNull(vm.error.value)
    }

    private fun artifact(name: String): String {
        val working = File(requireNotNull(System.getProperty("user.dir")))
        val root = if (File(working, "settings.gradle.kts").exists()) working else working.parentFile
        return File(root, ".build-outputs/$name").absolutePath
    }
}
