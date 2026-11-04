package com.example.learning

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelStore
import com.example.data.local.AppDatabase
import com.example.data.repository.LearningRepository
import com.example.ui.AdaptivePracticeViewModel
import com.example.ui.screens.AdaptiveLearningScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertNull
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveLearningUiTest {
    @get:Rule val compose = createComposeRule()
    private var db: AppDatabase? = null
    private val store = ViewModelStore()
    @After fun close() { store.clear(); db?.close() }

    @Test fun mapStartsPracticeAndCanResumeCheckedAnswer() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        db = database
        val repository = LearningRepository(database)
        runBlocking { repository.installDemo() }
        val vm = AdaptivePracticeViewModel(repository, "ui-student")
        store.put("adaptive", vm)
        compose.setContent { MyApplicationTheme { AdaptiveLearningScreen(vm, onBack = {}) } }
        compose.waitUntil(15000) { compose.waitForIdle(); vm.state.value.loaded }
        compose.onNodeWithText("Luyện điểm yếu").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15000) {
            compose.waitForIdle()
            vm.state.value.session?.currentQuestionId != null || vm.error.value != null
        }
        assertNull(vm.error.value, vm.error.value)
        compose.onNodeWithText("Vì sao chọn câu này?").assertExists()
        compose.onRoot().captureRoboImage(filePath = artifact("adaptive-practice.png"))
        val q = vm.state.value.bank.questions.first { it.id == vm.state.value.session?.currentQuestionId }
        compose.onNodeWithText(q.options[q.correctOptionIndex]).performScrollTo().performClick()
        compose.onNodeWithText("Kiểm tra").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.waitForIdle(); vm.state.value.session?.answered == true || vm.error.value != null }
        assertNull(vm.error.value, vm.error.value)
        compose.onNodeWithText("Chính xác!").assertExists()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Tiếp tục phiên luyện").performScrollTo().assertExists()
        compose.onNodeWithText("Tiếp tục phiên luyện").performClick()
        compose.onNodeWithText("Chính xác!").performScrollTo().assertExists()
        compose.onNodeWithText("Câu tiếp theo").performClick()
        compose.waitUntil(15000) { compose.waitForIdle(); vm.state.value.session?.currentQuestionId != q.id }
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Bản đồ kiến thức").assertExists()
        compose.onNodeWithTag("knowledge_map_list").performScrollToNode(hasText("Vòng đời Activity"))
        compose.onRoot().captureRoboImage(filePath = artifact("adaptive-map.png"))
        compose.onNodeWithText("Phân loại kỹ năng & độ khó").performScrollTo().performClick()
        compose.onNodeWithText("Phân loại câu hỏi").assertExists()
        compose.onNode(isToggleable()).performClick()
        compose.onRoot().captureRoboImage(filePath = artifact("adaptive-classification.png"))
        compose.onNodeWithContentDescription("Quay lại").performClick()
    }

    private fun artifact(name: String): String {
        val working = File(requireNotNull(System.getProperty("user.dir")))
        val root = if (File(working, "settings.gradle.kts").exists()) working else working.parentFile
        check(File(root, "settings.gradle.kts").exists())
        return File(root, ".build-outputs/$name").absolutePath
    }
}
