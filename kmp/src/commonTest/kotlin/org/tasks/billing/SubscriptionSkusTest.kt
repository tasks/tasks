package org.tasks.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionSkusTest {
    @Test
    fun readsThePriceOutOfANumberedSku() {
        assertEquals(30, SubscriptionSkus.subscriptionPrice("annual_30"))
        assertEquals(3, SubscriptionSkus.subscriptionPrice("monthly_03"))
        assertEquals(1, SubscriptionSkus.subscriptionPrice("annual_01"))
    }

    @Test
    fun fourNineNineIsTheLegacyFiveDollarTier() {
        assertEquals(5, SubscriptionSkus.subscriptionPrice("annual_499"))
    }

    @Test
    fun aSkuThatNamesNoPriceIsNotASubscription() {
        assertNull(SubscriptionSkus.subscriptionPrice("annual_tier"))
        assertFalse(SubscriptionSkus.isProSubscription("annual_tier"))
        assertFalse(SubscriptionSkus.isTasksSubscription("annual_tier"))
    }

    @Test
    fun theBillingPeriodIsThePrefix() {
        assertTrue(SubscriptionSkus.isMonthly("monthly_03"))
        assertFalse(SubscriptionSkus.isMonthly("annual_30"))
        assertFalse(SubscriptionSkus.isMonthly(null))
    }

    @Test
    fun theCloudFloorIsThreeAMonthOrThirtyAYear() {
        assertTrue(SubscriptionSkus.isTasksSubscription("monthly_03"))
        assertTrue(SubscriptionSkus.isTasksSubscription("annual_30"))
        assertFalse(SubscriptionSkus.isTasksSubscription("monthly_02"))
        assertFalse(SubscriptionSkus.isTasksSubscription("annual_29"))
    }

    @Test
    fun anythingThatIsNotASubscriptionIsNotOne() {
        assertFalse(SubscriptionSkus.isProSubscription("themes"))
        assertFalse(SubscriptionSkus.isProSubscription("vip"))
        assertFalse(SubscriptionSkus.isProSubscription(null))
        assertFalse(SubscriptionSkus.isTasksSubscription("debug_pro"))
    }
}
