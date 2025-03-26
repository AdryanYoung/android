package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import javax.inject.Inject

/**
 * Use case to delete a node attachment message.
 */
class DeletePendingMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val getTransferByUniqueIdUseCase: GetTransferByUniqueIdUseCase,
) : DeleteMessageUseCase() {

    override suspend fun deleteMessage(message: TypedMessage) {
        (message as? PendingAttachmentMessage)?.let {
            chatMessageRepository.deletePendingMessageById(it.msgId)
            it.transferUniqueId?.let { uniqueId ->
                getTransferByUniqueIdUseCase(uniqueId)?.let { transfer ->
                    cancelTransferByTagUseCase(transfer.tag)
                }
            }
        }
    }

    override suspend fun canDelete(message: TypedMessage) =
        message is PendingAttachmentMessage
}