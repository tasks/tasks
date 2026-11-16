package org.tasks.compose.pricing

import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import org.tasks.billing.SubscriptionProvider
import org.tasks.compose.accounts.Platform
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
        assertTrue(PricingMode.CLOUD_FIRST.satisfiedBy(nothing, supporter))
        assertTrue(PricingMode.SUPPORTER_FIRST.satisfiedBy(nothing, supporter))
    }

    @Test
    fun aSubscriptionTheUserArrivedWithIsNotSomethingTheyJustBought() {
        assertFalse(PricingMode.CLOUD_FIRST.satisfiedBy(supporter, supporter))
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(cloud, cloud))
    }

    @Test
    fun aFeaturePaywallHasNothingLeftToAskOfAnyoneWhoIsPaying() {
        assertTrue(PricingMode.SUPPORTER_FIRST.satisfiedBy(supporter, supporter, unlocksFeature = true))
        assertTrue(PricingMode.CLOUD_FIRST.satisfiedBy(cloud, cloud, unlocksFeature = true))
    }

    @Test
    fun aFeaturePaywallStillWantsSomethingFromAFreeUser() {
        assertFalse(PricingMode.SUPPORTER_FIRST.satisfiedBy(nothing, nothing, unlocksFeature = true))
    }

    @Test
    fun aCloudOnlyPaywallIsNotSatisfiedByProEvenForAFeature() {
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(supporter, supporter, unlocksFeature = true))
    }

    @Test
    fun aSponsorWhoBuysCloudHasBoughtSomething() {
        assertTrue(PricingMode.CLOUD_ONLY.satisfiedBy(supporter, cloud))
        assertTrue(PricingMode.CLOUD_FIRST.satisfiedBy(supporter, cloud))
    }

    @Test
    fun aCloudPaywallIsNotSatisfiedBySomethingThatIsNotCloud() {
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(nothing, supporter))
    }

    @Test
    fun aCloudSubscriptionThatLapsesLeavesTheCloudPaywallSellingAgain() {
        assertFalse(PricingMode.CLOUD_ONLY.satisfiedBy(cloud, nothing))
        assertFalse(PricingMode.CLOUD_FIRST.satisfiedBy(cloud, nothing))
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

    @Test
    fun proPlatformPaywallsUnlockAFeatureForExistingSubscribers() {
        val desktop = PlatformConfiguration(billingProvider = BillingProvider.STRIPE, thirdPartySyncRequiresPro = true)
        Platform.entries.filter { it != Platform.TASKS_ORG }.forEach {
            assertTrue(unlocksFeature(it.name, desktop), it.name)
        }
        listOf(Platform.TASKS_ORG.name, "settings", "themes", "icons").forEach {
            assertFalse(unlocksFeature(it, desktop), it)
        }
    }

    @Test
    fun thirdPartySyncOnlyUnlocksAFeatureWhenItRequiresPro() {
        val android = PlatformConfiguration(billingProvider = BillingProvider.GOOGLE_PLAY, thirdPartySyncRequiresPro = false)
        assertTrue(unlocksFeature(Platform.DAVX5.name, android))
        assertFalse(unlocksFeature(Platform.GOOGLE_TASKS.name, android))
        assertFalse(unlocksFeature(Platform.MICROSOFT.name, android))
    }

    companion object {
        private val nothing = PaywallEntitlement(pro = false, cloud = false)
        private val supporter = PaywallEntitlement(pro = true, cloud = false)
        private val cloud = PaywallEntitlement(pro = true, cloud = true)
    }
}
