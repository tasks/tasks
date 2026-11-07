package org.tasks.billing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionInfoTest {
    @Test
    fun aRungOnTheSupporterLadderIsASupporterSubscription() {
        assertTrue(subscription("annual_01").isSupporterSubscription)
        assertTrue(subscription("monthly_02").isSupporterSubscription)
    }

    @Test
    fun aCloudTierIsNotASupporterSubscription() {
        assertFalse(subscription("annual_30").isSupporterSubscription)
        assertFalse(subscription("monthly_03").isSupporterSubscription)
    }

    @Test
    fun aSkuThatNamesNoPriceIsNotASupporterSubscription() {
        assertFalse(subscription("desktop_play").isSupporterSubscription)
        assertFalse(subscription("debug_pro").isSupporterSubscription)
    }

    private fun subscription(sku: String) = SubscriptionProvider.SubscriptionInfo(
        sku = sku,
        isMonthly = SubscriptionSkus.isMonthly(sku),
        isTasksSubscription = SubscriptionSkus.isTasksSubscription(sku),
    )
}
