package org.tasks.billing

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.analytics.Firebase
import org.tasks.LocalBroadcastManager
import org.tasks.sync.SyncAdapters

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeStore()
    private val purchases = PlayPurchases(store, PlayPurchaseVerifier { true })
    private val billing = RecordingBilling()
    private val activity: Activity = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun upgradingHandsPlayTheSubscriptionItReplaces() = runTest(dispatcher) {
        store.saved = listOf(purchase("annual_01", "token-supporter"))
        purchases.load()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.purchase(activity, price = 30, monthly = false)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to "token-supporter"), billing.launches)
    }

    @Test
    fun aFirstPurchaseReplacesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.purchase(activity, price = 30, monthly = false)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to null), billing.launches)
    }

    @Test
    fun rebuyingTheSameSkuReplacesNothing() = runTest(dispatcher) {
        store.saved = listOf(purchase("annual_30", "token-cloud"))
        purchases.load()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.purchase(activity, price = 30, monthly = false)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to null), billing.launches)
    }

    @Test
    fun theSubscriptionAQueryTurnsUpDoesNotCloseTheScreen() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        purchases.replaceAll(listOf(purchase("annual_30", "token-cloud")))
        advanceUntilIdle()

        assertFalse(viewModel.viewState.value.purchased)
    }

    @Test
    fun aPurchaseClosesTheScreen() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        purchases.add(listOf(purchase("annual_30", "token-cloud")))
        advanceUntilIdle()

        assertTrue(viewModel.viewState.value.purchased)
    }

    private fun viewModel() = PurchaseActivityViewModel(
        savedStateHandle = SavedStateHandle(),
        inventory = mock(),
        billing = billing,
        playPurchases = purchases,
        localBroadcastManager = mock<LocalBroadcastManager>(),
        syncAdapters = mock<SyncAdapters>(),
        firebase = mock<Firebase>(),
    )

    private fun purchase(sku: String, token: String) = PlayPurchase(
        sku = sku,
        purchaseToken = token,
        originalJson = """{"productId":"$sku"}""",
        signature = "signature",
        isPurchased = true,
        isAcknowledged = true,
        isAutoRenewing = true,
    )

    private class FakeStore(var saved: List<PlayPurchase> = emptyList()) : PlayPurchaseStore {
        override suspend fun load() = saved

        override suspend fun save(purchases: List<PlayPurchase>) {
            saved = purchases
        }
    }

    private class RecordingBilling : PlayBilling {
        val launches = mutableListOf<Pair<String, String?>>()

        override suspend fun queryPurchases() = Unit

        override suspend fun products(skus: List<String>) = emptyList<PlayProductInfo>()

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            sku: String,
            subscription: Boolean,
            oldPurchaseToken: String?,
        ) {
            launches.add(sku to oldPurchaseToken)
        }

        override suspend fun acknowledge(purchase: PlayPurchase) = Unit

        override suspend fun consume(purchase: PlayPurchase) = Unit
    }
}
