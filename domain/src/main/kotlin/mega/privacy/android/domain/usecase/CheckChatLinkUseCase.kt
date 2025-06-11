package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import javax.inject.Inject

/**
 * Use case to Check Chat link.
 *
 * It also ensures that the user is logged in as Anonymous when required.
 */
class CheckChatLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
) {

    /**
     * Invoke
     *
     * @param chatLink  Chat Room link
     * @return          [ChatRequest]
     */
    suspend operator fun invoke(chatLink: String): ChatRequest {
        initChatGuestSessionIfNeeded()
        return chatRepository.checkChatLink(chatLink)
    }

    private suspend fun initChatGuestSessionIfNeeded() {
        runCatching {
            if (!isUserLoggedInUseCase()) {
                initGuestChatSessionUseCase(anonymousMode = true)
            }
        }.getOrNull()
    }
}
