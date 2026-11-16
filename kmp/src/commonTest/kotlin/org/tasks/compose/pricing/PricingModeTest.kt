package org.tasks.compose.pricing

import org.tasks.PlatformConfiguration
import org.tasks.analytics.AnalyticsEvents
import org.tasks.billing.BillingProvider
import org.tasks.compose.accounts.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingModeTest {
    @Test
    fun aFeatureThatOpenedThePaywallLeadsWithSupporter() {
        SupporterFeatures.entries.forEach {
            assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource(Platform.DAVX5.name, it), it.name)
            assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource("themes", it), it.name)
            assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource("icons", it), it.name)
        }
    }

    @Test
    fun aPlainUpgradeLeadsWithCloud() {
        SupporterFeatures.entries.filter { it != SupporterFeatures.ANDROID_CARD }.forEach {
            assertEquals(PricingMode.CLOUD_FIRST, PricingMode.forSource(AnalyticsEvents.SOURCE_SETTINGS, it), it.name)
            assertEquals(PricingMode.CLOUD_FIRST, PricingMode.forSource(SOURCE_MANAGE_SUBSCRIPTION, it), it.name)
        }
    }

    @Test
    fun theFDroidCardBuildLeadsWithSupporterEvenForAPlainUpgrade() {
        assertEquals(
            PricingMode.SUPPORTER_FIRST,
            PricingMode.forSource(AnalyticsEvents.SOURCE_SETTINGS, SupporterFeatures.ANDROID_CARD),
        )
    }

    @Test
    fun theFDroidCardBuildNeverRecommendsCloudEvenWhenAskedTo() {
        assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.CLOUD_FIRST.forFeatures(SupporterFeatures.ANDROID_CARD))
        assertEquals(PricingMode.CLOUD_ONLY, PricingMode.CLOUD_ONLY.forFeatures(SupporterFeatures.ANDROID_CARD))
        SupporterFeatures.entries.filter { it != SupporterFeatures.ANDROID_CARD }.forEach {
            assertEquals(PricingMode.CLOUD_FIRST, PricingMode.CLOUD_FIRST.forFeatures(it), it.name)
        }
    }

    @Test
    fun eachBuildSellsTheSupporterFeaturesItCanDeliver() {
        assertEquals(
            SupporterFeatures.APP_STORE,
            PlatformConfiguration(billingProvider = BillingProvider.GOOGLE_PLAY, isAndroid = true).supporterFeatures,
        )
        assertEquals(
            SupporterFeatures.ANDROID_CARD,
            PlatformConfiguration(billingProvider = BillingProvider.STRIPE, isAndroid = true).supporterFeatures,
        )
        assertEquals(
            SupporterFeatures.DESKTOP,
            PlatformConfiguration(billingProvider = BillingProvider.STRIPE).supporterFeatures,
        )
    }
}
