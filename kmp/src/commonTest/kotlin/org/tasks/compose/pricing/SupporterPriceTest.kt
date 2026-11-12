package org.tasks.compose.pricing

import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupporterPriceTest {
    @Test
    fun aKnownSupporterSubscriptionIsPricedWhileThePlanLoads() {
        val store = store(loadingPlan = true, knownSku = "annual_05")

        assertEquals("annual_05", store.supporterPrice?.sku)
        assertFalse(store.awaitingPlan)
    }

    @Test
    fun anUnknownPlanIsAwaitedRatherThanShownAsTheCheapestRung() {
        val store = store(loadingPlan = true)

        assertNull(store.supporterPrice)
        assertTrue(store.awaitingPlan)
    }

    @Test
    fun theLoadedPlanWinsOverTheKnownOne() {
        val store = store(currentSku = "annual_10", knownSku = "annual_05")

        assertEquals("annual_10", store.supporterPrice?.sku)
    }

    @Test
    fun noPlanOnceLoadedIsNotAwaited() {
        val store = store(knownSku = "annual_05")

        assertNull(store.supporterPrice)
        assertFalse(store.awaitingPlan)
    }

    @Test
    fun aCloudSubscriptionHasNoSupporterPrice() {
        assertNull(store(currentSku = "annual_30").supporterPrice)
    }

    private fun store(
        loadingPlan: Boolean = false,
        currentSku: String? = null,
        knownSku: String? = null,
    ) = StoreBilling(
        catalog = CATALOG,
        loadingPlan = loadingPlan,
        currentSku = currentSku,
        knownSku = knownSku,
        onSelect = {},
    )

    private companion object {
        val CATALOG = BillingCatalog.from(
            listOf(
                BillingOption("pri_01", "annual_01", 100),
                BillingOption("pri_05", "annual_05", 500),
                BillingOption("pri_10", "annual_10", 1000),
                BillingOption("pri_30", "annual_30", 3000, entitles = true),
            )
        )
    }
}
