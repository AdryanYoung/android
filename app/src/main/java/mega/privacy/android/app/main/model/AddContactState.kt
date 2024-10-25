package mega.privacy.android.app.main.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus

/**
 * State for Add Contact
 * @property chatId The chat id
 * @property isContactVerificationWarningEnabled contact verification flag is enabled or not
 * @property enabledFeatureFlags Set of enabled feature flags
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property currentChatCall                            [ChatCall]
 * @property accountType                                The account type
 * @property isBusinessAccountExpired                    True, if the business account is expired. False, otherwise.
 */
data class AddContactState(
    val chatId: Long? = null,
    val isContactVerificationWarningEnabled: Boolean = false,
    val enabledFeatureFlags: Set<Feature> = emptySet(),
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val currentChatCall: ChatCall? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
) {

    /**
     * Check if users limit is reached
     */
    private fun isUsersLimitInCallReached(): Boolean {
        callUsersLimit?.let { limit ->
            numParticipants?.let { participants ->
                return participants >= limit
            }
        }

        return false
    }

    /**
     * Num of participants
     */
    private val numParticipants
        get() = currentChatCall?.numParticipants

    /**
     * Get the users limit
     */
    private val callUsersLimit
        get() = currentChatCall?.callUsersLimit

    /**
     * Show user limit warning dialog
     */
    val showUserLimitWarningDialog
        get() = isCallUnlimitedProPlanFeatureFlagEnabled &&
                isUsersLimitInCallReached() &&
                currentChatCall?.status != ChatCallStatus.Destroyed
}
