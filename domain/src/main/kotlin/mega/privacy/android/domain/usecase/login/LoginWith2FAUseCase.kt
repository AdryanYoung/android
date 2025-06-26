package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for log in an account with 2FA enabled.
 */
class LoginWith2FAUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param password Account password.
     * @param pin2FA 2FA code.
     * @param disableChatApiUseCase [DisableChatApiUseCase].
     * @return Flow of [LoginStatus]
     */
    operator fun invoke(
        email: String,
        password: String,
        pin2FA: String,
        disableChatApiUseCase: DisableChatApiUseCase,
    ) = callbackFlow {
        runCatching {
            loginMutex.lock()
            loginRepository.multiFactorAuthLogin(email, password, pin2FA)
                .catch {
                    if (it !is LoginLoggedOutFromOtherLocation
                        && it !is LoginWrongMultiFactorAuth
                    ) {
                        chatLogoutUseCase(disableChatApiUseCase)
                        resetChatSettingsUseCase()
                    }
                    close(it)
                }
                .collectLatest { loginStatus ->
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        saveAccountCredentialsUseCase()
                    }
                    trySend(loginStatus)
                }
        }.onFailure {
            close(it)
        }

        awaitClose {
            runCatching { loginMutex.unlock() }
        }
    }
}
