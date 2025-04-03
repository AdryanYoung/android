package mega.privacy.android.app.presentation.overdisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Over disk quota paywall view model
 *
 */
@HiltViewModel
class OverDiskQuotaPaywallViewModel @Inject constructor(
    private val isDatabaseEntryStale: IsDatabaseEntryStale,
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
    private val getPricing: GetPricing,
    private val getUserDataUseCase: GetUserDataUseCase,
    monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
) : ViewModel() {
    private val _pricing = MutableStateFlow(Pricing(emptyList()))

    /**
     * Pricing
     */
    val pricing = _pricing.asStateFlow()

    /**
     * Monitor update user data broadcast event
     */
    val monitorUpdateUserData = monitorUpdateUserDataUseCase()

    /**
     * Monitor my account update broadcast event
     */
    val monitorMyAccountUpdate = monitorMyAccountUpdateUseCase()

    init {
        viewModelScope.launch {
            _pricing.value = runCatching { getPricing(false) }
                .getOrElse { Pricing(emptyList()) }
        }
    }

    /**
     * Request storage details only if is not already requested recently
     */
    fun requestStorageDetailIfNeeded() {
        viewModelScope.launch {
            if (isDatabaseEntryStale()) {
                runCatching {
                    getSpecificAccountDetailUseCase(storage = true, transfer = false, pro = false)
                }.onFailure {
                    Timber.w("Exception getting account detail: $it")
                }
            }
        }
    }

    /**
     * Request user's data
     */
    fun getUserData() = viewModelScope.launch {
        runCatching {
            getUserDataUseCase()
        }.onFailure {
            Timber.e("Failed to get the user's data: $it")
        }
    }
}