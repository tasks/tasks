package org.tasks.billing

import android.app.Activity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaySubscriptionProviderTest {
    private val stored = PlayPurchase(
        sku = "annual_30",
        purchaseToken = "token-annual_30",
        originalJson = "{}",
        signature = "signature",
        isPurchased = true,
        isAcknowledged = true,
        isAutoRenewing = true,
    )
    private val purchases = PlayPurchases(
        store = object : PlayPurchaseStore {
            override suspend fun load() = listOf(stored)

            override suspend fun save(purchases: List<PlayPurchase>) = Unit
        },
        verifier = PlayPurchaseVerifier { true },
    )
    private val provider = PlaySubscriptionProvider(purchases = purchases, billing = NoBilling)

    @Test
    fun theStoredPurchaseIsReadBeforeSyncing() = runTest {
        assertTrue(provider.awaitVerification())

        assertEquals("annual_30", provider.current?.sku)
        assertEquals("token-annual_30", provider.current?.purchaseToken)
    }

    @Test
    fun aPriceIsOnlyAskedForOncePerSession() = runTest {
        val billing = PricedBilling()
        val provider = PlaySubscriptionProvider(purchases = purchases, billing = billing)

        provider.getFormattedPrice("annual_30")
        assertEquals("$30.00", provider.getFormattedPrice("annual_30"))

        assertEquals(1, billing.calls)
    }

    @Test
    fun aMissingPriceIsAskedForAgain() = runTest {
        val billing = PricedBilling(price = null)
        val provider = PlaySubscriptionProvider(purchases = purchases, billing = billing)

        provider.getFormattedPrice("annual_30")
        provider.getFormattedPrice("annual_30")

        assertEquals(2, billing.calls)
    }

    private class PricedBilling(private val price: String? = "$30.00") : PlayBilling {
        var calls = 0

        override suspend fun queryPurchases() = Unit

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            sku: String,
            subscription: Boolean,
            oldPurchaseToken: String?,
        ) = Unit

        override suspend fun acknowledge(purchase: PlayPurchase) = Unit

        override suspend fun consume(purchase: PlayPurchase) = Unit

        override suspend fun products(skus: List<String>): List<PlayProductInfo> {
            calls++
            return listOfNotNull(
                price?.let {
                    PlayProductInfo(
                        productId = skus.first(),
                        formattedPrice = it,
                        priceAmountMicros = 30_000_000,
                        priceCurrencyCode = "USD",
                    )
                }
            )
        }
    }

    private object NoBilling : PlayBilling {
        override suspend fun queryPurchases() = Unit

        override suspend fun products(skus: List<String>) = emptyList<PlayProductInfo>()

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            sku: String,
            subscription: Boolean,
            oldPurchaseToken: String?,
        ) = Unit

        override suspend fun acknowledge(purchase: PlayPurchase) = Unit

        override suspend fun consume(purchase: PlayPurchase) = Unit
    }
}
