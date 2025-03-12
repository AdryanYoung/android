package mega.privacy.android.app.presentation.login.confirmemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.support.GenerateSupportEmailBodyUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [ConfirmEmailFragment]
 *
 * @property uiState View state as [ConfirmEmailUiState]
 */
@HiltViewModel
class ConfirmEmailViewModel @Inject constructor(
    private val monitorAccountConfirmationUseCase: MonitorAccountConfirmationUseCase,
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase,
    private val cancelCreateAccountUseCase: CancelCreateAccountUseCase,
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val generateSupportEmailBodyUseCase: GenerateSupportEmailBodyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmEmailUiState())
    val uiState: StateFlow<ConfirmEmailUiState> = _uiState

    init {
        viewModelScope.launch {
            monitorAccountConfirmationUseCase().collectLatest {
                _uiState.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.Login) }
            }
        }

        monitorConnectivity()

        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.RegistrationRevamp)
            }.onSuccess { isEnabled ->
                _uiState.update {
                    it.copy(isNewRegistrationUiEnabled = isEnabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
        }
    }

    /**
     * Update state with isPendingToShowFragment as null.
     */
    internal fun isPendingToShowFragmentConsumed() {
        _uiState.update { state -> state.copy(isPendingToShowFragment = null) }
    }

    /**
     * Resend the sign up link to the given email and full name
     *
     * @param email The email for the account
     * @param fullName The full name of the user
     */
    internal fun resendSignUpLink(email: String, fullName: String?) {
        viewModelScope.launch {
            Timber.d("Resending the sign-up link")
            _uiState.update { it.copy(isLoading = true) }
            runCatching { resendSignUpLinkUseCase(email = email, fullName = fullName) }
                .onSuccess { email ->
                    updateRegisteredEmail(email)
                    showSuccessSnackBar()
                }
                .onFailure { error ->
                    Timber.e("Failed to re-sent the sign up link", error)
                    if (error is MegaException) {
                        error.errorString?.let {
                            showErrorSnackBar(it)
                        }
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    internal fun cancelCreateAccount() {
        viewModelScope.launch {
            Timber.d("Cancelling the registration process")
            _uiState.update { it.copy(isLoading = true) }
            runCatching { cancelCreateAccountUseCase() }
                .onSuccess { email ->
                    updateRegisteredEmail(email)
                    showSuccessSnackBar()
                }
                .onFailure { error ->
                    Timber.e("Failed to cancel the registration process", error)
                    if (error is MegaException) {
                        error.errorString?.let {
                            showErrorSnackBar(it)
                        }
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateRegisteredEmail(email: String) {
        _uiState.update { it.copy(registeredEmail = email) }
        saveLastRegisteredEmail(email)
    }

    private fun showSuccessSnackBar() {
        _uiState.update { it.copy(shouldShowSuccessMessage = true) }
    }

    /**
     * Reset the success message visibility
     */
    internal fun onSuccessMessageDisplayed() {
        _uiState.update { it.copy(shouldShowSuccessMessage = false) }
    }

    private fun showErrorSnackBar(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * Reset the error message
     */
    internal fun onErrorMessageDisplayed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Save last registered email address to local storage
     */
    internal fun saveLastRegisteredEmail(email: String) {
        viewModelScope.launch {
            runCatching {
                saveLastRegisteredEmailUseCase(email)
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Generate the support email body
     */
    suspend fun generateSupportEmailBody() = runCatching {
        generateSupportEmailBodyUseCase()
    }.onFailure { Timber.e(it) }
        .getOrNull()
        .orEmpty()
}
