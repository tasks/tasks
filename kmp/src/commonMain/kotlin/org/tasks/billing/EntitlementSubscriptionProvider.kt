package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class EntitlementSubscriptionProvider(
    private val entitlement: PurchaseEntitlement,
    debugPro: Flow<Boolean> = flowOf(false),
    override val googleAndMicrosoftRequirePro: Boolean = false,
    private val verifyBeforeSync: Boolean = false,
) : SubscriptionProvider {

    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
        combine(
            entitlement.hasPro,
            entitlement.sku,
            entitlement.provider,
            entitlement.terms,
            debugPro,
        ) { hasPro, sku, provider, terms, debug ->
            when {
                hasPro -> SubscriptionProvider.SubscriptionInfo(
                    sku = sku ?: FALLBACK_SKU,
                    isMonthly = SubscriptionSkus.isMonthly(sku),
                    isTasksSubscription = SubscriptionSkus.isTasksSubscription(sku),
                    isGitHubSponsor = provider == EntitlementProvider.GITHUB_SPONSOR,
                    isCardSubscription = provider == EntitlementProvider.STRIPE,
                    terms = terms,
                )
                debug -> SubscriptionProvider.SubscriptionInfo(
                    sku = DEBUG_SKU,
                    isMonthly = false,
                    isTasksSubscription = false,
                    isGitHubSponsor = false,
                )
                else -> null
            }
        }

    override suspend fun awaitVerification(): Boolean =
        if (verifyBeforeSync) entitlement.awaitReady() else true

    override suspend fun checkTier(): Boolean = entitlement.checkTier()

    override suspend fun getFormattedPrice(sku: String): String? = entitlement.formattedPrice.first()

    companion object {
        private const val FALLBACK_SKU = "desktop_play"
        private const val DEBUG_SKU = "debug_pro"
    }
}
