package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property product list of Product subscriptions
 * @property cheapestSubscriptionAvailable cheapest subscription, which is available for user (Pro Lite or Pro I)
 * @property enableVariantAUI Boolean, to check if feature flag ChooseAccountScreenVariantA is enabled
 * @property enableVariantBUI Boolean, to check if feature flag ChooseAccountScreenVariantB is enabled
 * @property chosenPlan account type chosen by user (when user taps on one of the plans)
 * @property isPaymentMethodAvailable boolean to determine if Payments are available through Google Play Store
 * @property isMonthlySelected boolean to determine if monthly plan was selected
 * @property showAdsFeature boolean to determine if Ad-free feature should be shown or hidden (part of the In-App Advertisement experiment)
 * @property isProPromoRevampEnabled boolean to determine if Pro Promo Revamp feature should be shown or hidden
 * @constructor Create default Upgrade Account state
 */
data class ChooseAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription> = emptyList(),
    val product: List<Product> = emptyList(),
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
    val enableVariantAUI: Boolean = false,
    val enableVariantBUI: Boolean = false,
    val chosenPlan: AccountType = AccountType.FREE,
    val isPaymentMethodAvailable: Boolean = true,
    val isMonthlySelected: Boolean = false,
    val showAdsFeature: Boolean = false,
    val isProPromoRevampEnabled: Boolean = false,
)