package mega.privacy.android.app.presentation.chat.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.archived.model.ArchivedChatsState
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.usecase.chat.GetLastMessageUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.domain.entity.chat.ChatRoomItem

/**
 * Archived chats view model
 *
 * @property getChatsUseCase
 * @property getLastMessageUseCase
 * @property chatRoomTimestampMapper
 * @property archiveChatUseCase
 * @property getStringFromStringResMapper   [GetStringFromStringResMapper]
 */
@HiltViewModel
class ArchivedChatsViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
) : ViewModel() {

    private val state = MutableStateFlow(ArchivedChatsState())

    fun getState(): StateFlow<ArchivedChatsState> = state

    init {
        getArchivedChats()
    }

    private fun getArchivedChats() =
        viewModelScope.launch {
            getChatsUseCase(
                chatRoomType = ChatRoomType.ARCHIVED_CHATS,
                lastMessage = getLastMessageUseCase::invoke,
                lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
                meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
                headerTimeMapper = chatRoomTimestampMapper::getHeaderTimeFormatted,
            )
                .conflate()
                .catch { Timber.e(it) }
                .collect { items ->
                    state.update {
                        it.copy(items = items)
                    }
                }
        }

    /**
     * Unarchive chat
     *
     * @param chatId
     */
    fun unarchiveChat(chatId: Long) {
        viewModelScope.launch {
            val chatTitle = state.value.items.firstOrNull { it.chatId == chatId }?.let {
                if (it is ChatRoomItem.NoteToSelfChatRoomItem) getStringFromStringResMapper(
                    sharedR.string.chat_note_to_self_chat_title
                ) else it.title
            }

            runCatching { archiveChatUseCase(chatId, false) }
                .onSuccess {
                    val snackBarItem = SnackBarItem(
                        stringRes = R.string.success_unarchive_chat,
                        stringArg = chatTitle
                    )
                    state.update { it.copy(snackBar = snackBarItem) }
                }
                .onFailure { exception ->
                    Timber.e(exception)
                    val snackBarItem = SnackBarItem(
                        stringRes = R.string.error_unarchive_chat,
                        stringArg = chatTitle
                    )
                    state.update { it.copy(snackBar = snackBarItem) }
                }
        }
    }

    /**
     * Dismiss SnackBar
     */
    fun dismissSnackBar() {
        state.update {
            it.copy(snackBar = null)
        }
    }
}
