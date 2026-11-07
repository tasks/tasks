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
