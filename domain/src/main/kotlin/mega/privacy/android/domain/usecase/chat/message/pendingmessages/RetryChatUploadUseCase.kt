package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.flow.collect
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import javax.inject.Inject

/**
 * Use case to retry the upload of a chat message.
 */
class RetryChatUploadUseCase @Inject constructor(
    private val getPendingMessageUseCase: GetPendingMessageUseCase,
    private val startChatUploadsWithWorkerUseCase: StartChatUploadsWithWorkerUseCase,
    private val getOrCreateMyChatsFilesFolderIdUseCase: GetOrCreateMyChatsFilesFolderIdUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(chatUploadAppData: List<TransferAppData.ChatUpload>) {
        if (chatUploadAppData.isEmpty()) {
            throw ChatUploadNotRetriedException()
        }

        val pendingMessages = chatUploadAppData
            .mapNotNull { getPendingMessageUseCase(it.pendingMessageId) }

        if (pendingMessages.isEmpty()) {
            throw ChatUploadNotRetriedException()
        }

        pendingMessages.first().let { pendingMessage ->
            startChatUploadsWithWorkerUseCase(
                uriPath = pendingMessage.uriPath,
                chatFilesFolderId = getOrCreateMyChatsFilesFolderIdUseCase(),
                *chatUploadAppData.map { it.pendingMessageId }.toLongArray()
            ).collect()
        }
    }
}