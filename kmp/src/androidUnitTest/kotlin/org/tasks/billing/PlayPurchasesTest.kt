package org.tasks.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPurchasesTest {
    private val store = FakePlayPurchaseStore()

    @Test
    fun monthlyIsPro() = runTest {
        val purchases = withPurchases(purchase("monthly_01"))

        assertTrue(purchases.hasPro)
        assertEquals(1, purchases.state.value.subscription?.subscriptionPrice)
    }

    @Test
    fun theDearerMonthlyWins() = runTest {
        val purchases = withPurchases(purchase("monthly_01"), purchase("monthly_03"))

        assertEquals(3, purchases.state.value.subscription?.subscriptionPrice)
    }

    @Test
    fun monthlyBeatsAnnual() = runTest {
        val purchases = withPurchases(purchase("monthly_01"), purchase("annual_03"))

        assertTrue(purchases.state.value.subscription!!.isMonthly)
    }

    @Test
    fun aCancelledSubscriptionIsStillPro() = runTest {
        val purchases = withPurchases(purchase("annual_03", autoRenewing = false))

        assertTrue(purchases.state.value.subscription!!.isCanceled)
        assertTrue(purchases.hasPro)
    }

    @Test
    fun stillRenewingBeatsCancelled() = runTest {
        val purchases = withPurchases(
            purchase("annual_03", autoRenewing = false),
            purchase("annual_05"),
        )

        assertFalse(purchases.state.value.subscription!!.isCanceled)
    }

    @Test
    fun aPendingPurchaseStillCountsAsPro() = runTest {
        val purchases = withPurchases(purchase("monthly_01", purchased = false))

        assertTrue(purchases.hasPro)
    }

    @Test
    fun addKeepsWhatPlayDidNotMention() = runTest {
        val purchases = withPurchases(purchase("monthly_01"))

        purchases.add(listOf(purchase("annual_03")))

        assertEquals(setOf("monthly_01", "annual_03"), purchases.state.value.purchases.keys)
    }

    @Test
    fun replaceAllDropsWhatPlayNoLongerReports() = runTest {
        val purchases = withPurchases(purchase("monthly_01"), purchase("annual_03"))

        purchases.replaceAll(listOf(purchase("annual_03")))

        assertEquals(setOf("annual_03"), purchases.state.value.purchases.keys)
    }

    @Test
    fun aOneTimeThemesPurchaseUnlocksThemesButNotPro() = runTest {
        val purchases = withPurchases(purchase(PlayPurchase.SKU_THEMES))

        assertFalse(purchases.hasPro)
        assertTrue(purchases.purchasedThemes)
    }

    @Test
    fun aVipPurchaseIsPro() = runTest {
        val purchases = withPurchases(purchase(PlayPurchase.SKU_VIP))

        assertTrue(purchases.hasPro)
        assertNull(purchases.state.value.subscription)
    }

    @Test
    fun anUnacknowledgedPurchaseIsQueuedForAcknowledgement() = runTest {
        val purchases = withPurchases(purchase("monthly_01", acknowledged = false))

        assertEquals(listOf("monthly_01"), purchases.needingAcknowledgement.map { it.sku })
    }

    @Test
    fun anAcknowledgedPurchaseIsNotQueuedAgain() = runTest {
        assertTrue(withPurchases(purchase("monthly_01")).needingAcknowledgement.isEmpty())
    }

    @Test
    fun aPendingPurchaseIsNotAcknowledgedYet() = runTest {
        val purchases = withPurchases(
            purchase("monthly_01", purchased = false, acknowledged = false)
        )

        assertTrue(purchases.needingAcknowledgement.isEmpty())
    }

    @Test
    fun purchasesSurviveARestart() = runTest {
        withPurchases(purchase("monthly_03"))

        val restarted = PlayPurchases(store, acceptAll)
        restarted.load()

        assertTrue(restarted.hasPro)
        assertEquals("monthly_03", restarted.state.value.subscription?.sku)
    }

    @Test
    fun readingTheStoreNeverReplacesFresherPurchases() = runTest {
        val purchases = PlayPurchases(store, acceptAll)
        purchases.replaceAll(listOf(purchase("annual_30")))

        purchases.load()

        assertEquals(setOf("annual_30"), purchases.state.value.purchases.keys)
    }

    @Test
    fun addReportsOnlyWhatItAccepted() = runTest {
        val purchases = PlayPurchases(store, PlayPurchaseVerifier { it.sku == "annual_30" })

        val accepted = purchases.add(listOf(purchase("annual_30"), purchase("annual_01")))

        assertEquals(listOf("annual_30"), accepted.map { it.sku })
    }

    @Test
    fun anUnverifiableStoredPurchaseIsNotTrustedOnRestart() = runTest {
        PlayPurchases(store, acceptAll).replaceAll(listOf(purchase("monthly_01")))

        val restarted = PlayPurchases(store, PlayPurchaseVerifier { false })
        restarted.load()

        assertFalse(restarted.hasPro)
    }

    @Test
    fun anUnverifiablePurchaseIsDiscardedOnArrival() = runTest {
        val purchases = PlayPurchases(store, PlayPurchaseVerifier { false })
        purchases.replaceAll(listOf(purchase("monthly_01")))

        assertFalse(purchases.hasPro)
        assertNull(purchases.state.value.subscription)
    }

    @Test
    fun theStoreAnnouncingANewPurchaseIsAPurchase() = runTest {
        val purchases = PlayPurchases(store, acceptAll)

        val announced = purchases.announcementsWhile(this) {
            purchases.add(listOf(purchase("annual_30")))
        }

        assertEquals(1, announced)
    }

    @Test
    fun readingBackThePurchasesTheStoreAlreadyHadIsNotAPurchase() = runTest {
        val purchases = PlayPurchases(store, acceptAll)

        val announced = purchases.announcementsWhile(this) {
            purchases.replaceAll(listOf(purchase("annual_30")))
        }

        assertEquals(0, announced)
    }

    @Test
    fun theStoreRepeatingAPurchaseWeAlreadyHaveIsNotAPurchase() = runTest {
        val purchases = withPurchases(purchase("annual_30"))

        val announced = purchases.announcementsWhile(this) {
            purchases.add(listOf(purchase("annual_30")))
        }

        assertEquals(0, announced)
    }

    @Test
    fun replacingASubscriptionWithANewTokenIsAPurchase() = runTest {
        val purchases = withPurchases(purchase("annual_30"))

        val announced = purchases.announcementsWhile(this) {
            purchases.add(
                listOf(purchase("annual_30").copy(purchaseToken = "token-replacement"))
            )
        }

        assertEquals(1, announced)
    }

    @Test
    fun anUnverifiablePurchaseIsNotAnnounced() = runTest {
        val purchases = PlayPurchases(store, PlayPurchaseVerifier { false })

        val announced = purchases.announcementsWhile(this) {
            purchases.add(listOf(purchase("annual_30")))
        }

        assertEquals(0, announced)
    }

    private suspend fun PlayPurchases.announcementsWhile(
        scope: CoroutineScope,
        block: suspend () -> Unit,
    ): Int {
        val announced = mutableListOf<Unit>()
        val collector = scope.launch(Dispatchers.Unconfined) {
            purchased.collect { announced.add(it) }
        }
        block()
        collector.cancel()
        return announced.size
    }

    @Test
    fun anUpdateThatArrivedBeforeAnyoneWasListeningIsNotLost() = runTest {
        val purchases = withPurchases(purchase("monthly_01"))
        val updates = mutableListOf<Unit>()

        val collector = launch(Dispatchers.Unconfined) {
            purchases.updates.collect { updates.add(it) }
        }
        collector.cancel()

        assertEquals(1, updates.size)
    }

    private suspend fun withPurchases(vararg items: PlayPurchase) =
        PlayPurchases(store, acceptAll).also { it.replaceAll(items.toList()) }

    private fun purchase(
        sku: String,
        purchased: Boolean = true,
        acknowledged: Boolean = true,
        autoRenewing: Boolean = true,
    ) = PlayPurchase(
        sku = sku,
        purchaseToken = "token-$sku",
        originalJson = """{"productId":"$sku"}""",
        signature = "signature-$sku",
        isPurchased = purchased,
        isAcknowledged = acknowledged,
        isAutoRenewing = autoRenewing,
    )

    private class FakePlayPurchaseStore : PlayPurchaseStore {
        private var saved = emptyList<PlayPurchase>()

        override suspend fun load() = saved

        override suspend fun save(purchases: List<PlayPurchase>) {
            saved = purchases
        }
    }

    companion object {
        private val acceptAll = PlayPurchaseVerifier { true }
    }
}
