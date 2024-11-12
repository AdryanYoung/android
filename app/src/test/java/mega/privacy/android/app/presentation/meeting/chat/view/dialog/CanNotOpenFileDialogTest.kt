package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanNotOpenFileDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that title and description are displayed`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.dialog_cannot_open_file_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.dialog_cannot_open_file_text))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.context_download))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.general_dialog_cancel_button))
    }

    private fun initComposeRuleContent() {
        composeRule.setContent {
            CanNotOpenFileDialog(
                onDownloadClick = {},
                onDismiss = {}
            )
        }
    }
}