package org.tasks.billing

import android.app.Activity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.tasks.InMemoryDataStore
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
    private val subscriptionCenter = mutableListOf<String>()

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
    fun anUnreachableStoreLeavesThePricesUnavailable() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakePlayBilling(productsError = PlayBillingUnavailableException("SERVICE_UNAVAILABLE"))
        )
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertFalse(state.loadingPrices)
        assertNull(state.error)
        val store = state.toStoreBilling(suggestedPrice = null) {}
        assertTrue(store.unavailable(store.catalog.cloudAnnual))
    }

    @Test
    fun aFailedCatalogLoadIsRetriedOnce() = runTest(dispatcher) {
        val billing = FakePlayBilling(products = CATALOG, productsFailures = 1)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        assertEquals(2, billing.productsCalls)
        assertEquals("annual_30", viewModel.viewState.value.catalog.cloudAnnual?.sku)
        assertFalse(viewModel.viewState.value.loadingPrices)
        assertFalse(reporting.events.contains(AnalyticsEvents.BILLING_FLOW_RESULT))
    }

    @Test
    fun aCatalogThatKeepsFailingStopsRetrying() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            productsError = PlayBillingUnavailableException("SERVICE_UNAVAILABLE"),
        )
        viewModel(billing)
        advanceUntilIdle()

        assertEquals(PlayCheckoutViewModel.CATALOG_ATTEMPTS, billing.productsCalls)
        assertTrue(reporting.events.contains(AnalyticsEvents.BILLING_FLOW_RESULT))
    }

    @Test
    fun theCatalogDoesNotWaitForPurchases() = runTest(dispatcher) {
        val billing = FakePlayBilling(products = CATALOG, queryGate = CompletableDeferred())
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        assertFalse(viewModel.viewState.value.loadingPrices)
        assertEquals("annual_30", viewModel.viewState.value.catalog.cloudAnnual?.sku)
        billing.queryGate!!.complete(Unit)
    }

    @Test
    fun aFailedQueryIsNotShownToTheBuyer() = runTest(dispatcher) {
        val viewModel = viewModel(
            FakePlayBilling(products = CATALOG, queryError = PlayBillingUnavailableException("ERROR"))
        )
        advanceUntilIdle()

        assertNull(viewModel.viewState.value.error)
        assertEquals("annual_30", viewModel.viewState.value.catalog.cloudAnnual?.sku)
    }

    @Test
    fun retryingAFailedPurchaseLaunchesItAgain() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnavailableException("SERVICE_DISCONNECTED"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()
        val annual = viewModel.viewState.value.catalog.cloudAnnual!!

        viewModel.select(annual, activity)
        advanceUntilIdle()
        assertEquals(
            PlayCheckoutViewModel.Error.Unavailable(annual),
            viewModel.viewState.value.error,
        )

        billing.launchError = null
        viewModel.select(viewModel.viewState.value.error!!.price, activity)
        advanceUntilIdle()

        assertNull(viewModel.viewState.value.error)
        assertEquals(listOf("annual_30" to null), billing.launches)
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
    fun resubscribingToACanceledSkuOpensThePlaySubscriptionCenter() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30", autoRenewing = false)))
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        assertEquals(listOf("annual_30"), subscriptionCenter)
        assertTrue(billing.launches.isEmpty())
        assertNull(viewModel.viewState.value.busySku)
    }

    @Test
    fun aCanceledSubscriptionCanStillChangePlan() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30", autoRenewing = false)))
        val billing = FakePlayBilling(products = CATALOG)
        val viewModel = viewModel(billing)
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudMonthly!!, activity)
        advanceUntilIdle()

        assertTrue(subscriptionCenter.isEmpty())
        assertEquals(listOf("monthly_03" to "token-30"), billing.launches)
    }

    @Test
    fun retryLogsTheClickAndLaunchesAgain() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnavailableException("SERVICE_DISCONNECTED"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()
        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()
        billing.launchError = null

        viewModel.retry(activity)
        advanceUntilIdle()

        assertNull(viewModel.viewState.value.error)
        assertEquals(listOf("annual_30" to null), billing.launches)
        assertTrue(reporting.events.contains(AnalyticsEvents.PRICING_SUBSCRIBE_CLICK))
    }

    @Test
    fun anUnrecoverableFailureIsNotRetried() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnrecoverableException("ITEM_UNAVAILABLE"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()
        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()
        billing.launchError = null

        viewModel.retry(activity)
        advanceUntilIdle()

        assertNull(viewModel.viewState.value.error)
        assertTrue(billing.launches.isEmpty())
    }

    @Test
    fun retryDoesNotBuyASkuThatIsNowOwned() = runTest(dispatcher) {
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnavailableException("SERVICE_DISCONNECTED"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()
        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()
        billing.launchError = null
        purchases.replaceAll(listOf(purchase("annual_30", "token-30")))
        advanceUntilIdle()

        viewModel.retry(activity)
        advanceUntilIdle()

        assertTrue(billing.launches.isEmpty())
    }

    @Test
    fun aCompletedPlanChangeIsReported() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("monthly_03", "token-monthly")))
        val viewModel = viewModel(FakePlayBilling(products = CATALOG))
        advanceUntilIdle()

        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()
        assertFalse(viewModel.viewState.value.purchased)

        purchases.replaceAll(listOf(purchase("annual_30", "token-annual")))
        advanceUntilIdle()
        assertTrue(viewModel.viewState.value.purchased)

        viewModel.purchaseHandled()
        assertFalse(viewModel.viewState.value.purchased)
    }

    @Test
    fun aFailedLaunchIsNotReportedAsAPurchase() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("monthly_03", "token-monthly")))
        val billing = FakePlayBilling(
            products = CATALOG,
            launchError = PlayBillingUnavailableException("SERVICE_DISCONNECTED"),
        )
        val viewModel = viewModel(billing)
        advanceUntilIdle()
        viewModel.select(viewModel.viewState.value.catalog.cloudAnnual!!, activity)
        advanceUntilIdle()

        purchases.replaceAll(listOf(purchase("annual_30", "token-annual")))
        advanceUntilIdle()

        assertFalse(viewModel.viewState.value.purchased)
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
            PlayCheckoutViewModel.Error.Failed(
                message = "ITEM_UNAVAILABLE",
                price = viewModel.viewState.value.catalog.cloudAnnual!!,
            ),
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

    @Test
    fun aCanceledSubscriptionKeepsItsSkuSelectable() = runTest(dispatcher) {
        purchases.replaceAll(listOf(purchase("annual_30", "token-30", autoRenewing = false)))
        val viewModel = viewModel(FakePlayBilling(products = CATALOG))
        advanceUntilIdle()

        val store = viewModel.viewState.value.toStoreBilling(suggestedPrice = null) {}
        assertTrue(store.selectable(store.catalog.cloudAnnual))
    }

    private fun viewModel(
        billing: PlayBilling,
        coveredBy: UnlockSource? = null,
    ) = PlayCheckoutViewModel(
        billing = billing,
        purchases = purchases,
        reporting = reporting,
        coveredBy = flowOf(coveredBy),
        openSubscriptionCenter = { _, sku -> subscriptionCenter.add(sku) },
    )

    private fun purchase(sku: String, token: String, autoRenewing: Boolean = true) = PlayPurchase(
        sku = sku,
        purchaseToken = token,
        originalJson = "{}",
        signature = "signature",
        isPurchased = true,
        isAcknowledged = true,
        isAutoRenewing = autoRenewing,
    )

    private class FakePlayBilling(
        var products: List<PlayProductInfo> = emptyList(),
        var productsError: Exception? = null,
        var productsFailures: Int = 0,
        var launchError: Exception? = null,
        private val queryError: Exception? = null,
        val queryGate: CompletableDeferred<Unit>? = null,
    ) : PlayBilling {
        var queries = 0
        val launches = mutableListOf<Pair<String, String?>>()

        override suspend fun queryPurchases() {
            queries++
            queryGate?.await()
            queryError?.let { throw it }
        }

        var productsCalls = 0

        override suspend fun products(skus: List<String>): List<PlayProductInfo> {
            productsCalls++
            productsError?.let { throw it }
            if (productsFailures > 0) {
                productsFailures--
                throw PlayBillingUnavailableException("SERVICE_DISCONNECTED")
            }
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

        override val analyticsEnabled = true

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
