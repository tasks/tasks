package org.tasks.compose.pricing

import org.tasks.analytics.AnalyticsEvents
import org.tasks.compose.accounts.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingModeTest {
    @Test
    fun aFeatureThatOpenedThePaywallLeadsWithSupporter() {
        assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource(Platform.DAVX5.name))
        assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource("themes"))
        assertEquals(PricingMode.SUPPORTER_FIRST, PricingMode.forSource("icons"))
    }

    @Test
    fun aPlainUpgradeLeadsWithCloud() {
        assertEquals(PricingMode.CLOUD_FIRST, PricingMode.forSource(AnalyticsEvents.SOURCE_SETTINGS))
        assertEquals(PricingMode.CLOUD_FIRST, PricingMode.forSource(SOURCE_MANAGE_SUBSCRIPTION))
    }

    @Test
    fun theFDroidCardBuildLeadsWithSupporter() {
        assertEquals(
            PricingMode.SUPPORTER_FIRST,
            PricingMode.CLOUD_FIRST.forFeatures(SupporterFeatures.ANDROID_CARD),
        )
    }

    @Test
    fun otherBuildsKeepTheOrderTheSourceChose() {
        SupporterFeatures.entries.filter { it != SupporterFeatures.ANDROID_CARD }.forEach {
            assertEquals(PricingMode.CLOUD_FIRST, PricingMode.CLOUD_FIRST.forFeatures(it), it.name)
        }
    }

    @Test
    fun aCloudOnlyPaywallStaysCloudOnly() {
        SupporterFeatures.entries.forEach {
            assertEquals(PricingMode.CLOUD_ONLY, PricingMode.CLOUD_ONLY.forFeatures(it), it.name)
        }
    }
}
