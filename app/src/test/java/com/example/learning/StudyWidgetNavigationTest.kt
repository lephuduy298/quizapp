package com.example.learning

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.MainActivity
import com.example.QuizApplication
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudyWidgetNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<QuizApplication>()
    private fun intent() = Intent(app, MainActivity::class.java).setAction(MainActivity.ACTION_WIDGET_REVIEW)
    private fun waitForReview() {
        compose.waitUntil(15000) { compose.onAllNodesWithTag("review_list").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun widgetRequestSurvivesLoginAndAnotherTapAfterActivityRecreation() {
        val manager = app.container.userManager
        manager.register("widgetnav", "testpassword")
        manager.logout()
        ActivityScenario.launch<MainActivity>(intent()).use { scenario ->
            compose.onNodeWithTag("username_input").performTextInput("widgetnav")
            compose.onNodeWithTag("password_input").performTextInput("testpassword")
            compose.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
            waitForReview()
            scenario.recreate()
            waitForReview()
            compose.onNodeWithContentDescription("Quay lại").performClick()
            // A new SINGLE_TOP intent must navigate from the study hub back to review.
            scenario.onActivity {
                MainActivity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
                    .apply { isAccessible = true }.invoke(it, intent())
            }
            waitForReview()
        }
    }

    @Test fun reviewNotificationWaitsForLoginAndOpensTheDueReviewScreen() {
        val manager = app.container.userManager
        manager.register("reviewnotice", "testpassword")
        manager.logout()
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)
            .setAction(MainActivity.ACTION_REVIEW_REMINDER)).use {
            compose.onNodeWithTag("username_input").performTextInput("reviewnotice")
            compose.onNodeWithTag("password_input").performTextInput("testpassword")
            compose.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
            waitForReview()
            compose.onNodeWithText("Đến hạn", substring = false).assertIsSelected()
        }
    }

    @Test fun reviewNotificationOpensColdAndResetsTheTabWhenAlreadyRunning() {
        val manager = app.container.userManager
        manager.register("reviewwarm", "testpassword")
        manager.login("reviewwarm", "testpassword")
        val request = Intent(app, MainActivity::class.java).setAction(MainActivity.ACTION_REVIEW_REMINDER)
        ActivityScenario.launch<MainActivity>(request).use { scenario ->
            waitForReview()
            compose.onNodeWithText("Câu sai", substring = false).performClick()
            scenario.onActivity {
                MainActivity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
                    .apply { isAccessible = true }.invoke(it, request)
            }
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Đến hạn", substring = false)
                    .filter(isSelected()).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test fun coldLaunchOpensReviewForAnAuthenticatedUser() {
        val manager = app.container.userManager
        manager.register("widgetcold", "testpassword")
        manager.login("widgetcold", "testpassword")
        ActivityScenario.launch<MainActivity>(intent()).use { waitForReview() }
    }

    @Test fun studyHubOpensEachDestinationAndWidgetReturnsToDueTab() {
        val manager = app.container.userManager
        manager.register("studyhub", "testpassword")
        manager.login("studyhub", "testpassword")
        val scenario = ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java))
        compose.onNodeWithTag("study_hub_entry").performScrollTo().performClick()
        compose.onNodeWithTag("study_hub").assertExists()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("0 câu từng sai", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        val working = File(requireNotNull(System.getProperty("user.dir")))
        val root = if (File(working, "settings.gradle.kts").exists()) working else working.parentFile
        compose.onRoot().captureRoboImage(filePath = File(root, ".build-outputs/study-hub.png").absolutePath)
        compose.onNodeWithTag("study_mistakes").performScrollTo().performClick()
        waitForReview()
        compose.onNodeWithText("Câu sai", substring = false).assertIsSelected()
        scenario.recreate()
        waitForReview()
        compose.onNodeWithText("Câu sai", substring = false).assertIsSelected()
        // Widget taps must reset the tab even when the review screen is already open.
        scenario.onActivity {
            MainActivity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
                .apply { isAccessible = true }.invoke(it, intent())
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Đến hạn", substring = false)
                .filter(isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("study_due_review").performScrollTo().performClick()
        waitForReview()
        compose.onNodeWithText("Đến hạn", substring = false).assertIsSelected()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("study_practice").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("knowledge_map_list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("study_hub").assertExists()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("study_hub_entry").assertExists()
        scenario.onActivity { it.finish() }
        shadowOf(Looper.getMainLooper()).idle()
        // Robolectric resets the scenario after this test; close() blocks after recreation.
    }
}
