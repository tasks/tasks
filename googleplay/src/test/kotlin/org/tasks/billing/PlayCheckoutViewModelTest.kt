package org.tasks.billing

import android.app.Activity
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.preferences.TasksPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class PlayCheckoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val purchases = PlayPurchases(
        JsonPlayPurchaseStore(preferences),
        PlayPurchaseVerifier { true },
    )
    private val activity: Activity = mock()
    private val reporting = RecordingReporting(preferences)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theCatalogIsLoadedFromPlay() = runTest(dispatcher) {
        val viewModel = viewModel(FakePlayBilling(products = CATALOG))
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertFalse(state.loadingPrices)
        assertEquals("annual_30", state.catalog.cloudAnnual?.sku)
        assertEquals("monthly_03", state.catalog.cloudMonthly?.sku)
        assertEquals(listOf("annual_05", "annual_10"), state.catalog.supporterRungs.map { it.sku })
        assertNull(state.error)
    }

    @Test
    fun purchasesAreRefreshedWhenThePaywallOpens() = runTest(dispatcher) {
        val billing = FakePlayBilling(products = CATALOG)
        viewModel(billing)
        advanceUntilIdle()

        assertEquals(1, billing.queries)
    }

    @Test
    fun theOwnedSkuIsReported() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30")))
        val viewModel = viewModel(FakePlayBilling(products = CATALOG))
        advanceUntilIdle()

        assertEquals("annual_30", viewModel.viewState.value.currentSku)
    }

    @Test
    fun anUnreachableStoreIsAnUnavailableError() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakePlayBilling(productsError = PlayBillingUnavailableException("SERVICE_UNAVAILABLE"))
        )
        advanceUntilIdle()

        assertEquals(PlayCheckoutViewModel.Error.Unavailable, viewModel.viewState.value.error)
        assertFalse(viewModel.viewState.value.loadingPrices)
    }

    @Test
    fun retryReloadsTheCatalog() = runTest(dispatcher) {
        val billing = FakePlayBilling(productsError = PlayBillingUnavailableException("ERROR"))
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        billing.productsError = null
        billing.products = CATALOG
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.viewState.value.error)
        assertEquals("annual_30", viewModel.viewState.value.catalog.cloudAnnual?.sku)
    }

    @Test
    fun selectingAPriceLaunchesThePlayFlow() = runTest(dispatcher) {
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to null), billing.launches)
        assertNull(viewModel.viewState.value.busySku)
    }

    @Test
    fun changingPlanHandsPlayTheOldToken() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("monthly_03", "token-monthly")))
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to "token-monthly"), billing.launches)
    }

    @Test
    fun rebuyingTheSameSkuDoesNotSendAnOldToken() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30")))
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to null), billing.launches)
    }

    @Test
    fun aFailedLaunchIsReportedAndClearsBusy() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnrecoverableException("ITEM_UNAVAILABLE"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        assertEquals(
            PlayCheckoutViewModel.Error.Failed("ITEM_UNAVAILABLE"),
            viewModel.viewState.value.error,
        )
        assertNull(viewModel.viewState.value.busySku)
        assertTrue(reporting.events.contains(AnalyticsEvents.CHECKOUT_ERROR))
    }

    @Test
    fun aSecondSelectionIsIgnoredWhileOneIsInFlight() = runTest(dispatcher) {
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        val annual = viewModel.viewState.value.catalog.cloudAnnual!!
        val monthly = viewModel.viewState.value.catalog.cloudMonthly!!
        viewModel.select(annual, activity)
        viewModel.select(monthly, activity)
        advanceUntilIdle()

        assertEquals(listOf("annual_30" to null), billing.launches)
    }

    @Test
    fun aSubscriptionBoughtElsewhereIsSurfaced() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakePlayBilling(products = CATALOG),
            coveredBy = UnlockSource.STRIPE,
        )
        advanceUntilIdle()

        assertTrue(viewModel.viewState.value.coveredElsewhere)
        assertEquals(UnlockSource.STRIPE, viewModel.viewState.value.coveredBy)
    }

    @Test
    fun playNeverResellsWhatIsAlreadyOwned() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30")))
        val viewModel = viewModel(FakePlayBilling(products = CATALOG))
        advanceUntilIdle()

        val store = viewModel.viewState.value.toStoreBilling(suggestedPrice = null) {}
        assertFalse(store.sellsCurrentSku)
        assertFalse(store.selectable(store.catalog.cloudAnnual))
        assertTrue(store.selectable(store.catalog.cloudMonthly))
    }

    private fun viewModel(
        billing: PlayBilling,
        coveredBy: UnlockSource? = null,
    ) = PlayCheckoutViewModel(
        billing = billing,
        purchases = purchases,
        reporting = reporting,
        coveredBy = kotlinx.coroutines.flow.flowOf(coveredBy),
    )

    private fun purchase(sku: String, token: String) = PlayPurchase(
        sku = sku,
        purchaseToken = token,
        originalJson = "{}",
        signature = "signature",
        isPurchased = true,
        isAcknowledged = true,
        isAutoRenewing = true,
    )

    private class FakePlayBilling(
        var products: List<PlayProductInfo> = emptyList(),
        var productsError: Exception? = null,
        private val launchError: Exception? = null,
    ) : PlayBilling {
        var queries = 0
        val launches = mutableListOf<Pair<String, String?>>()

        override suspend fun queryPurchases() {
            queries++
        }

        override suspend fun products(skus: List<String>): List<PlayProductInfo> {
            productsError?.let { throw it }
            return products.filter { skus.contains(it.productId) }
        }

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            sku: String,
            subscription: Boolean,
            oldPurchaseToken: String?,
        ) {
            launchError?.let { throw it }
            launches.add(sku to oldPurchaseToken)
        }

        override suspend fun acknowledge(purchase: PlayPurchase) = Unit

        override suspend fun consume(purchase: PlayPurchase) = Unit
    }

    private class RecordingReporting(
        override val tasksPreferences: TasksPreferences,
    ) : Reporting {
        val events = mutableListOf<String>()

        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            events.add(event)
        }

        override fun addTask(source: String) = Unit

        override fun completeTask(source: String) = Unit

        override fun setSku(sku: String?) = Unit

        override fun reportException(t: Throwable, fatal: Boolean) = Unit
    }

    companion object {
        private val CATALOG = listOf(
            product("annual_30", 30_000_000),
            product("monthly_03", 3_000_000),
            product("annual_05", 5_000_000),
            product("annual_10", 10_000_000),
        )

        private fun product(productId: String, micros: Long) = PlayProductInfo(
            productId = productId,
            formattedPrice = "$${micros / 1_000_000}.00",
            priceAmountMicros = micros,
            priceCurrencyCode = "USD",
        )
    }
}
