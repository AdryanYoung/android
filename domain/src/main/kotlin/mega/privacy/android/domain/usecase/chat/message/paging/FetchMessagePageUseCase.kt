package mega.privacy.android.domain.usecase.chat.message.paging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.usecase.chat.message.GetMessageListUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import javax.inject.Inject

/**
 * Fetch message page use case
 *
 * @property loadMessagesUseCase
 * @property getMessageListUseCase
 * @property monitorChatRoomMessagesUseCase
 */
class FetchMessagePageUseCase @Inject constructor(
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val getMessageListUseCase: GetMessageListUseCase,
    private val monitorChatRoomMessagesUseCase: MonitorChatRoomMessagesUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId
     * @param coroutineScope
     * @return FetchMessagePageResponse
     */
    suspend operator fun invoke(
        chatId: Long,
        coroutineScope: CoroutineScope,
    ): FetchMessagePageResponse {
        val messageFlow = monitorChatRoomMessagesUseCase(chatId).shareIn(
            scope = coroutineScope, started = SharingStarted.Eagerly
        )

        return kotlinx.coroutines.coroutineScope {
            val messageResponse =
                async {
                    runCatching { getMessageListUseCase(messageFlow) }
                        .getOrElse {
                            if (it is TimeoutCancellationException) {
                                emptyList()
                            } else throw it
                        }
                }
            val loadResponse = async { loadMessagesUseCase(chatId) }
            return@coroutineScope FetchMessagePageResponse(
                chatId = chatId,
                messages = messageResponse.await(),
                loadResponse = loadResponse.await(),
            )
        }
    }
}
