package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnableGeolocationDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that correct dialog is shown`() {
        initComposeRuleContent()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_ENABLE_GEOLOCATION_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.title_activity_maps))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.explanation_send_location))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_continue)).assertIsDisplayed()
            onNodeWithText(activity.getString(sharedR.string.general_dialog_cancel_button)).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent() {
        composeRule.setContent {
            EnableGeolocationDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}