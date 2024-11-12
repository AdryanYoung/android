package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class JoinAnswerCallDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val holdPressed = mock<() -> Unit>()
    private val endPressed = mock<() -> Unit>()
    private val cancelPressed = mock<() -> Unit>()

    @Test
    fun `test that correct dialog is shown when chat is a group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_another_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_join_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.end_and_join_call_incoming).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when chat is a group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_join_call_incoming).assertDoesNotExist()
            onNodeWithText(R.string.end_and_join_call_incoming).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when chat is not a group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_one_to_one_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_another_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_answer_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.end_and_answer_call_incoming).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when chat is not a group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_one_to_one_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_answer_call_incoming).assertDoesNotExist()
            onNodeWithText(R.string.end_and_answer_call_incoming).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that hold and join is invoked if button is pressed when chat is group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.hold_and_join_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(holdPressed).invoke()
        }
    }

    @Test
    fun `test that hold and answer is invoked if button is pressed when chat is not group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.hold_and_answer_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(holdPressed).invoke()
        }
    }

    @Test
    fun `test that end and join is invoked if button is pressed when chat is group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.end_and_join_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that end and join is invoked if button is pressed when chat is group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.end_and_join_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that end and answer is invoked if button is pressed when chat is not group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.end_and_answer_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that end and answer is invoked if button is pressed when chat is not group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithText(R.string.end_and_answer_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = true,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is not group and there is only one call in other chat`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 1,
            )
        )
        with(composeRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is not group and there are two calls in other chats`() {
        initComposeRuleContent(
            JoinAnswerCallDialogStatus(
                isGroup = false,
                numberOfCallsInOtherChats = 2,
            )
        )
        with(composeRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    private fun initComposeRuleContent(status: JoinAnswerCallDialogStatus) {
        composeRule.setContent {
            with(status) {
                JoinAnswerCallDialog(
                    isGroup = isGroup,
                    numberOfCallsInOtherChats = numberOfCallsInOtherChats,
                    onHoldAndAnswer = holdPressed,
                    onEndAndAnswer = endPressed,
                    onDismiss = cancelPressed,
                )
            }
        }
    }
}