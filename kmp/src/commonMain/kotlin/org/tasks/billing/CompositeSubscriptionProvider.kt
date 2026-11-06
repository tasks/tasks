package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeSubscriptionProvider(
    private val providers: List<SubscriptionProvider>,
) : SubscriptionProvider {
    init {
        require(providers.isNotEmpty()) { "No subscription providers" }
    }

    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
        combine(providers.map { it.subscription }) { infos ->
            infos.filterNotNull().minWithOrNull(PRECEDENCE)
        }

    override suspend fun getFormattedPrice(sku: String): String? =
        providers.firstNotNullOfOrNull { it.getFormattedPrice(sku) }

    override suspend fun awaitVerification(): Boolean = providers.all { it.awaitVerification() }

    override suspend fun checkTier(): Boolean = providers.any { it.checkTier() }

    override val googleAndMicrosoftRequirePro: Boolean
        get() = providers.any { it.googleAndMicrosoftRequirePro }

    companion object {
        private val PRECEDENCE =
            compareByDescending<SubscriptionProvider.SubscriptionInfo> { it.isTasksSubscription }
                .thenByDescending { it.purchaseToken != null }
    }
}
