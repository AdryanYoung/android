package mega.privacy.android.app.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.app.presentation.chat.model.MediaPlayerOpenedErrorState
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaChatMessage
import timber.log.Timber
import javax.inject.Inject


/**
 * View Model for [mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity]
 */
@HiltViewModel
class NodeAttachmentHistoryViewModel @Inject constructor(
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val copyChatNodesUseCase: CopyChatNodesUseCase,
    private val megaNavigator: MegaNavigator,
) : ViewModel() {

    private val _snackbarMessageEvent = MutableStateFlow<Int?>(null)

    /**
     * Flow of [Int] res id to show a snackbar message.
     */
    val snackbarMessageEvent = _snackbarMessageEvent.asStateFlow()

    private val _startChatFileOfflineDownloadEvent = MutableStateFlow<ChatFile?>(null)

    /**
     * Flow of [ChatFile] to start the download of a chat file.
     */
    val startChatFileOfflineDownloadEvent = _startChatFileOfflineDownloadEvent.asStateFlow()

    private val _copyResultFlow = MutableStateFlow<CopyRequestState?>(null)

    /**
     * Flow of [CopyRequestState] to notify the result of the copy operation.
     */
    val copyResultFlow = _copyResultFlow.asStateFlow()
    private val _mediaPlayerOpenedErrorFlow = MutableStateFlow<MediaPlayerOpenedErrorState?>(null)

    /**
     * Flow of [MediaPlayerOpenedErrorState] to notify the error when opening the media player.
     */
    val mediaPlayerOpenedErrorFlow = _mediaPlayerOpenedErrorFlow.asStateFlow()

    private val _collisionsFlow = MutableStateFlow<List<NameCollision>>(emptyList())

    /**
     * Flow of [NameCollisionUiEntity] to notify the name collisions
     */
    val collisionsFlow = _collisionsFlow.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param chatId            Chat ID where the node is.
     * @param messageIds        Message IDs where the node is.
     * @param newParentHandle   Parent handle in which the nodes will be copied.
     */
    fun importChatNodes(
        chatId: Long,
        messageIds: List<Long>,
        newParentHandle: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = messageIds,
                    newNodeParent = NodeId(newParentHandle),
                )
            }.onSuccess { result ->
                result.collisionResult.conflictNodes.values
                    .filterIsInstance<NodeNameCollision.Chat>()
                    .takeIf { it.isNotEmpty() }
                    ?.let { _collisionsFlow.update { _ -> it } }

                result.moveRequestResult?.let {
                    _copyResultFlow.update { _ ->
                        CopyRequestState(result = it.toCopyRequestResult())
                    }
                }
            }.onFailure { throwable ->
                Timber.e(throwable)
                _copyResultFlow.update {
                    CopyRequestState(error = throwable)
                }
            }
        }
    }

    /**
     * Clears the copy result after consuming the value
     */
    fun copyResultConsumed() {
        _copyResultFlow.value = null
    }

    /**
     * Clears the collisions after consuming the value
     */
    fun nodeCollisionsConsumed() {
        _collisionsFlow.update {
            emptyList()
        }
    }

    /**
     * Update the [MediaPlayerOpenedErrorState] value
     *
     * @param value [MediaPlayerOpenedErrorState]
     */
    fun updateMediaPlayerOpenedError(value: MediaPlayerOpenedErrorState?) =
        _mediaPlayerOpenedErrorFlow.update { value }

    /**
     * Open media player
     *
     * @param context Context
     * @param handle node handle
     * @param message [MegaChatMessage]
     * @param chatId chat ID
     * @param name node name
     */
    fun openMediaPlayer(
        context: Context,
        handle: Long,
        message: MegaChatMessage,
        chatId: Long,
        name: String,
        position: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                val nodeContentUri = getNodeContentUriByHandleUseCase(handle)
                megaNavigator.openMediaPlayerActivityFromChat(
                    context = context,
                    contentUri = nodeContentUri,
                    handle = handle,
                    messageId = message.msgId,
                    chatId = chatId,
                    name = name
                )
            }.recover { throwable ->
                Timber.e(throwable)
                updateMediaPlayerOpenedError(
                    MediaPlayerOpenedErrorState(
                        message = message,
                        position = position,
                        error = throwable
                    )
                )
            }
        }
    }

    /**
     * Save chat node to offline
     *
     * @param chatId    Chat ID where the node is.
     * @param messageId Message ID where the node is.
     */
    fun saveChatNodeToOffline(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            runCatching {
                val chatFile = getChatFileUseCase(chatId = chatId, messageId = messageId)
                    ?: throw IllegalStateException("Chat file not found")
                val isAvailableOffline = isAvailableOfflineUseCase(chatFile)
                if (isAvailableOffline) {
                    _snackbarMessageEvent.emit(R.string.file_already_exists)
                } else {
                    _startChatFileOfflineDownloadEvent.emit(chatFile)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Remove chat node from offline
     *
     * @param nodeId node ID
     */
    fun removeChatNodeFromOffline(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                removeOfflineNodeUseCase(nodeId)
            }
        }
    }

    /**
     * Consume the snackbar message event
     */
    fun onSnackbarMessageConsumed() {
        viewModelScope.launch {
            _snackbarMessageEvent.emit(null)
        }
    }

    /**
     * Consume the start chat file offline download event
     */
    fun onStartChatFileOfflineDownloadEventConsumed() {
        viewModelScope.launch {
            _startChatFileOfflineDownloadEvent.emit(null)
        }
    }

    fun copyAttachmentsToForward(
        chatId: Long,
        messageIdsToCopy: List<Long>,
        newParentHandle: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                copyChatNodesUseCase(
                    chatId = chatId,
                    messageIds = messageIdsToCopy,
                    newNodeParent = NodeId(newParentHandle),
                )
            }.onSuccess { result ->
                _copyResultFlow.update { _ ->
                    CopyRequestState(result = result.toCopyRequestResult())
                }
            }.onFailure { throwable ->
                Timber.e(throwable)
                _copyResultFlow.update {
                    CopyRequestState(error = throwable)
                }
            }
        }

    }
}
