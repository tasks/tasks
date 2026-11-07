package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
                hasPro -> info(sku, provider, terms)
                debug -> DEBUG_INFO
                else -> null
            }
        }

    override val current: SubscriptionProvider.SubscriptionInfo?
        get() = if (entitlement.hasPro.value) {
            info(entitlement.sku.value, entitlement.provider.value, entitlement.terms.value)
        } else {
            null
        }

    override suspend fun awaitVerification(): Boolean =
        if (verifyBeforeSync) entitlement.awaitReady() else true

    override suspend fun checkTier(): Boolean = entitlement.checkTier()

    override suspend fun getFormattedPrice(sku: String): String? =
        entitlement.formattedPrice.value.takeIf { sku == current?.sku }

    private fun info(
        sku: String?,
        provider: EntitlementProvider?,
        terms: PurchaseTerms?,
    ) = SubscriptionProvider.SubscriptionInfo(
        sku = sku ?: FALLBACK_SKU,
        isMonthly = SubscriptionSkus.isMonthly(sku),
        isTasksSubscription = SubscriptionSkus.isTasksSubscription(sku),
        provider = provider,
        terms = terms,
    )

    companion object {
        private const val FALLBACK_SKU = "desktop_play"
        private const val DEBUG_SKU = "debug_pro"

        private val DEBUG_INFO = SubscriptionProvider.SubscriptionInfo(
            sku = DEBUG_SKU,
            isMonthly = false,
            isTasksSubscription = false,
        )
    }
}
