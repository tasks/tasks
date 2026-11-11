package org.tasks.compose.pricing

import org.tasks.analytics.AnalyticsEvents
import org.tasks.compose.accounts.Platform
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeadsWithSupporterTest {
    @Test
    fun aFeatureThatOpenedThePaywallLeadsWithSupporter() {
        assertTrue(leadsWithSupporter(Platform.DAVX5.name))
        assertTrue(leadsWithSupporter("themes"))
        assertTrue(leadsWithSupporter("icons"))
    }

    @Test
    fun aPlainUpgradeLeadsWithCloud() {
        assertFalse(leadsWithSupporter(AnalyticsEvents.SOURCE_SETTINGS))
        assertFalse(leadsWithSupporter(SOURCE_MANAGE_SUBSCRIPTION))
    }
}
