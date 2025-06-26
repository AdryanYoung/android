package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out from chat api.
 */
class ChatLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {

    /**
     * Invoke.
     *
     * @param disableChatApiUseCase Temporary param for disabling megaChatApi.
     */
    suspend operator fun invoke(disableChatApiUseCase: DisableChatApiUseCase? = null) {
        runCatching { loginRepository.chatLogout() }
            .onSuccess {
                disableChatApiUseCase?.invoke()
            }
    }
}