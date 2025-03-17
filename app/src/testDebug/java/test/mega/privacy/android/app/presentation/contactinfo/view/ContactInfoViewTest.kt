package mega.privacy.android.app.presentation.contactinfo.view

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoUiState
import mega.privacy.android.app.presentation.contactinfo.view.ContactInfoView
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ContactInfoViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val testHandle = 123456L
    private val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
        userVisibility = UserVisibility.Unknown,
    )
    private val contactItem = ContactItem(
        handle = testHandle,
        email = "test@gmail.com",
        contactData = contactData,
        defaultAvatarColor = "red",
        visibility = UserVisibility.Visible,
        timestamp = 123456789,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
        lastSeen = 0,
        chatroomId = null,
    )
    private val contactItemWithVerifiedCredentials = ContactItem(
        handle = testHandle,
        email = "test@gmail.com",
        contactData = contactData,
        defaultAvatarColor = "red",
        visibility = UserVisibility.Visible,
        timestamp = 123456789,
        areCredentialsVerified = true,
        status = UserChatStatus.Online,
        lastSeen = 0,
        chatroomId = null,
    )
    private val chatRoom = mock<ChatRoom> {
        on { chatId }.thenReturn(123456L)
        on { changes }.thenReturn(null)
        on { title }.thenReturn("Chat title")
    }

    private val contactState = ContactInfoUiState(
        error = null,
        userChatStatus = UserChatStatus.Online,
        lastGreen = 0,
        isFromContacts = false,
        avatar = null,
        contactItem = contactItem,
        chatRoom = chatRoom,
        isOnline = false,
        snackBarMessage = null,
        snackBarMessageString = null,
        isUserRemoved = false,
        callStatusChanged = false,
        isPushNotificationSettingsUpdated = false,
        shouldNavigateToChat = false,
        isChatNotificationChange = false,
        isStorageOverQuota = false,
        isNodeUpdated = false,
        isCopyInProgress = false,
        nameCollisions = emptyList(),
        copyError = null,
        inShares = emptyList(),
    )

    private fun setupRule(state: ContactInfoUiState = ContactInfoUiState()) {
        composeRule.setContent {
            ContactInfoView(
                uiState = state,
                onBackPress = {},
                statusBarHeight = 25f,
                updateNickName = {},
                updateNickNameDialogVisibility = {}
            )
        }
    }

    @Test
    fun `test that contact info shows all elements when chatRoom is created`() {
        setupRule(contactState)
        composeRule.onNodeWithText(fromId(R.string.online_status)).assertExists()
        //IncomingSharesView
        composeRule.onNodeWithText(fromId(R.string.title_incoming_shares_explorer)).assertExists()
        //ChatNotificationsView
        composeRule.onNodeWithText(fromId(R.string.title_properties_chat_notifications_contact))
            .assertExists()
        //ShareContactView
        composeRule.onNodeWithText(fromId(R.string.title_properties_chat_share_contact))
            .assertExists()
        //VerifyCredentialsView
        composeRule.onNodeWithText(fromId(R.string.contact_approve_credentials_toolbar_title))
            .assertExists()
        //SharedFilesView
        composeRule.onNodeWithText(fromId(R.string.title_chat_shared_files_info)).assertExists()
        //ManageChatHistoryView
        composeRule.onNodeWithText(fromId(R.string.title_properties_manage_chat)).assertExists()
        //RemoveContactView
        composeRule.onNodeWithText(fromId(R.string.title_properties_remove_contact)).assertExists()
        composeRule.onNodeWithText(contactData.alias ?: return).assertExists()
        composeRule.onNodeWithText(contactData.fullName ?: return).assertExists()
        composeRule.onNodeWithText(contactData.fullName ?: return).assertExists()
    }

    @Test
    fun `test that contact info does not show shared files and manage history when chatRoom is not created`() {
        setupRule(
            ContactInfoUiState(
                contactItem = contactItem,
                userChatStatus = UserChatStatus.Online
            )
        )
        composeRule.onNodeWithText(fromId(R.string.online_status)).assertExists()
        //IncomingSharesView
        composeRule.onNodeWithText(fromId(R.string.title_incoming_shares_explorer)).assertExists()
        //ChatNotificationsView
        composeRule.onNodeWithText(fromId(R.string.title_properties_chat_notifications_contact))
            .assertExists()
        //ShareContactView
        composeRule.onNodeWithText(fromId(R.string.title_properties_chat_share_contact))
            .assertExists()
        //VerifyCredentialsView
        composeRule.onNodeWithText(fromId(R.string.contact_approve_credentials_toolbar_title))
            .assertExists()
        //SharedFilesView
        composeRule.onNodeWithText(fromId(R.string.title_chat_shared_files_info))
            .assertDoesNotExist()
        //ManageChatHistoryView
        composeRule.onNodeWithText(fromId(R.string.title_properties_manage_chat))
            .assertDoesNotExist()
        //RemoveContactView
        composeRule.onNodeWithText(fromId(R.string.title_properties_remove_contact)).assertExists()
        composeRule.onNodeWithText(contactData.alias ?: return).assertExists()
        composeRule.onNodeWithText(contactData.fullName ?: return).assertExists()
        composeRule.onNodeWithText(contactData.fullName ?: return).assertExists()
    }

    @Test
    fun `test that the verified icon is shown when the credentials are verified`() {
        setupRule(ContactInfoUiState(contactItem = contactItemWithVerifiedCredentials))
        composeRule.onNodeWithText(R.string.contact_verify_credentials_verified_text).assertExists()
        composeRule.onNodeWithTag(IconPackR.drawable.ic_contact_verified.toString()).assertExists()
    }

    @Test
    fun `test that the unverified icon is shown when the credentials are verified`() {
        setupRule(ContactInfoUiState(contactItem = contactItem))
        composeRule.onNodeWithText(R.string.contact_verify_credentials_not_verified_text)
            .assertExists()
    }

    @Test
    fun `test that the Online status with green icon is shown when the user status is Online`() {
        setupRule(contactState)
        composeRule.onNodeWithText(R.string.online_status).assertExists()
        composeRule.onNodeWithTag(R.drawable.ic_online_light.toString()).assertExists()
    }
}