package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.meeting.ChatInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.utils.Constants
import timber.log.Timber

fun showGroupOrContactInfoActivity(context: Context, uiState: ChatUiState) {
    with(uiState) {
        if (schedIsPending && isActive) {
            Timber.d("show scheduled meeting info")
            Intent(context, ChatInfoActivity::class.java).apply {
                putExtra(Constants.CHAT_ID, scheduledMeeting?.chatId)
                putExtra(Constants.SCHEDULED_MEETING_ID, scheduledMeeting?.schedId)
            }.also {
                context.startActivity(it)
            }
        } else {
            val targetActivity =
                if (isGroup) GroupChatInfoActivity::class.java else ContactInfoActivity::class.java
            Intent(context, targetActivity).apply {
                putExtra(Constants.HANDLE, chatId)
            }.also {
                context.startActivity(it)
            }
        }
    }
}