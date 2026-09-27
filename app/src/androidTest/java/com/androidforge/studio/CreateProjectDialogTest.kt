package com.androidforge.studio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.androidforge.studio.ui.home.CreateProjectDialog
import com.androidforge.studio.ui.theme.AndroidForgeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for key studio dialogs. Run with:
 * `./gradlew :app:connectedAndroidTest` (device/emulator required).
 */
class CreateProjectDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dialogShowsTemplatesAndCreateButton() {
        rule.setContent {
            AndroidForgeTheme {
                CreateProjectDialog(creating = false, onDismiss = {}, onCreate = { _, _, _ -> })
            }
        }
        rule.onNodeWithText("New project").assertIsDisplayed()
        rule.onNodeWithText("Project name").assertIsDisplayed()
        rule.onNodeWithText("Package name").assertIsDisplayed()
        rule.onNodeWithText("Empty Compose").assertIsDisplayed()
        rule.onNodeWithText("XML Layout").assertIsDisplayed()
        rule.onNodeWithText("Create").assertIsDisplayed()
    }

    @Test
    fun userCanTypeProjectNameAndConfirm() {
        var capturedName = ""
        rule.setContent {
            AndroidForgeTheme {
                CreateProjectDialog(
                    creating = false,
                    onDismiss = {},
                    onCreate = { name, _, _ -> capturedName = name },
                )
            }
        }
        rule.onNodeWithText("Project name").performTextInput("MyGame")
        rule.onNodeWithText("Create").performClick()
        rule.waitForIdle()
        assertEquals("MyGame", capturedName)
    }
}
