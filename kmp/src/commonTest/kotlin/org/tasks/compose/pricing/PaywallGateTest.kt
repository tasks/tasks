package org.tasks.compose.pricing

import org.tasks.billing.SubscriptionProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaywallGateTest {
    @Test
    fun aFreeUserWhoSubscribesToCloudHasWhatEveryPaywallWasSelling() {
        PricingMode.entries.forEach { mode ->
            assertTrue(mode.satisfiedBy(nothing, cloud), mode.name)
        }
    }

    @Test
    fun aFreeUserWhoNamesAPriceHasWhatThosePaywallsWereSelling() {
        assertTrue(PricingMode.NYP_ONLY.satisfiedBy(nothing, supporter))
        assertTrue(PricingMode.BOTH.satisfiedBy(nothing, supporter))
    }

    @Test
    fun aSubscriptionTheUserArrivedWithIsNotSomethingTheyJustBought() {
        assertFalse(PricingMode.BOTH.satisfiedBy(supporter, supporter))
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(cloud, cloud))
    }

    @Test
    fun aNameYourPricePaywallHasNothingLeftToAskOfAnyoneWhoIsPaying() {
        assertTrue(PricingMode.NYP_ONLY.satisfiedBy(supporter, supporter))
    }

    @Test
    fun aSponsorWhoBuysCloudHasBoughtSomething() {
        assertTrue(PricingMode.CLOUD_ONLY.satisfiedBy(supporter, cloud))
        assertTrue(PricingMode.BOTH.satisfiedBy(supporter, cloud))
    }

    @Test
    fun aCloudPaywallIsNotSatisfiedBySomethingThatIsNotCloud() {
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(nothing, supporter))
    }

    @Test
    fun aCloudSubscriptionThatLapsesLeavesTheCloudPaywallSellingAgain() {
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(cloud, nothing))
        assertFalse(PricingMode.BOTH.satisfiedBy(cloud, nothing))
        assertFalse(PricingMode.NYP_ONLY.satisfiedBy(cloud, nothing))
    }

    @Test
    fun aSubscriptionIsEverythingAPaywallSellsAndACloudOneIsBoth() {
        assertEquals(nothing, PaywallEntitlement.of(null))
        assertEquals(
            supporter,
            PaywallEntitlement.of(
                SubscriptionProvider.SubscriptionInfo(
                    sku = "annual_10",
                    isMonthly = false,
                    isTasksSubscription = false,
                )
            )
        )
        assertEquals(
            cloud,
            PaywallEntitlement.of(
                SubscriptionProvider.SubscriptionInfo(
                    sku = "annual_30",
                    isMonthly = false,
                    isTasksSubscription = true,
                )
            )
        )
    }

    companion object {
        private val nothing = PaywallEntitlement(pro = false, cloud = false)
        private val supporter = PaywallEntitlement(pro = true, cloud = false)
        private val cloud = PaywallEntitlement(pro = true, cloud = true)
    }
}
