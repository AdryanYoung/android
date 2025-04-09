package mega.privacy.android.app.presentation.chat.list.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

/**
 * Chats tab state
 *
 * @property chats
 * @property areChatsLoading
 * @property meetings
 * @property areMeetingsLoading
 * @property selectedIds
 * @property currentChatStatus
 * @property currentUnreadStatus
 * @property currentCallChatId
 * @property currentWaitingRoom
 * @property searchQuery
 * @property snackBar
 * @property isParticipatingInChatCallResult
 * @property tooltip
 * @property snackbarMessageContent
 * @property showForceUpdateDialog
 * @property hasArchivedChats
 * @property hasAnyContact
 * @constructor Create empty Chat tab state
 */
data class ChatsTabState(
    val chats: List<ChatRoomItem> = emptyList(),
    val areChatsLoading: Boolean = true,
    val meetings: List<ChatRoomItem> = emptyList(),
    val areMeetingsLoading: Boolean = true,
    val selectedIds: List<Long> = emptyList(),
    val currentChatStatus: ChatStatus? = null,
    val currentUnreadStatus: Pair<Boolean, Boolean>? = null,
    val currentCallChatId: Long? = null,
    val currentWaitingRoom: Long? = null,
    val searchQuery: String? = null,
    val snackBar: SnackBarItem? = null,
    val isParticipatingInChatCallResult: Boolean? = null,
    val tooltip: MeetingTooltipItem = MeetingTooltipItem.NONE,
    val snackbarMessageContent: StateEventWithContent<Int> = consumed(),
    val showForceUpdateDialog: Boolean = false,
    val hasArchivedChats: Boolean = false,
    val hasAnyContact: Boolean = false,
    val currentTab: ChatTab = ChatTab.CHATS,
    val onlyNoteToSelfChat: Boolean = false,
    val areChatsOrMeetingLoading: Boolean = true,
    val isEmptyChatsOrMeetings: Boolean = false

) {
    /**
     * Check if the chats list is empty
     */
    val noChats
        get() = chats.isEmpty()

    /**
     * Check if the meeting list is empty
     */
    val noMeetings
        get() = meetings.isEmpty()
}

