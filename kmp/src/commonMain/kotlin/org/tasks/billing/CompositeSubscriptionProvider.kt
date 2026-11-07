package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeSubscriptionProvider(
    private val providers: List<SubscriptionProvider>,
) : SubscriptionProvider {
    private val logger = Logger.withTag("CompositeSubscriptionProvider")

    init {
        require(providers.isNotEmpty()) { "No subscription providers" }
    }

    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
        combine(providers.map { it.subscription }) { infos ->
            infos.filterNotNull().minWithOrNull(PRECEDENCE)
        }

    override val current: SubscriptionProvider.SubscriptionInfo?
        get() = providers.mapNotNull { it.current }.minWithOrNull(PRECEDENCE)

    override suspend fun getFormattedPrice(sku: String): String? {
        val owner = providers.firstOrNull { it.current?.sku == sku }
        return owner?.priceOrNull(sku)
            ?: providers.filterNot { it === owner }.firstNotNullOfOrNull { it.priceOrNull(sku) }
    }

    override suspend fun awaitVerification(): Boolean =
        providers.map { it.awaitVerification() }.all { it }

    override suspend fun checkTier(): Boolean = providers.any { it.checkTier() }

    override val googleAndMicrosoftRequirePro: Boolean
        get() = providers.any { it.googleAndMicrosoftRequirePro }

    private suspend fun SubscriptionProvider.priceOrNull(sku: String): String? = try {
        getFormattedPrice(sku)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.w(e) { "Failed to read the price of $sku" }
        null
    }

    companion object {
        private val PRECEDENCE =
            compareByDescending<SubscriptionProvider.SubscriptionInfo> { it.isTasksSubscription }
                .thenByDescending { it.purchaseToken != null }
    }
}
