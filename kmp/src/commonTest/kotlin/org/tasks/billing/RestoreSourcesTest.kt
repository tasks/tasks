package org.tasks.billing

import org.tasks.PlatformConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class RestoreSourcesTest {

    @Test
    fun aDesktopOffersEveryWayIn() {
        assertEquals(
            listOf(UnlockSource.GOOGLE_PLAY, UnlockSource.GITHUB, UnlockSource.STRIPE),
            restoreSources(
                PlatformConfiguration(
                    billingProvider = BillingProvider.STRIPE,
                    supportsPlayLinkRestore = true,
                )
            ),
        )
    }

    @Test
    fun aPhoneThatCanUnlockADesktopIsNotOfferedTheLinkFlowItself() {
        assertEquals(
            listOf(UnlockSource.GITHUB, UnlockSource.STRIPE),
            restoreSources(
                PlatformConfiguration(
                    billingProvider = BillingProvider.GOOGLE_PLAY,
                    isAndroid = true,
                    supportsDesktopLinking = true,
                )
            ),
        )
    }
}
