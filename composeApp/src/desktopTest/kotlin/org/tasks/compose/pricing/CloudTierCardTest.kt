package org.tasks.compose.pricing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption

@OptIn(ExperimentalTestApi::class)
class CloudTierCardTest {

    private val annual = BillingOption(
        priceId = "pri_30",
        sku = "annual_30",
        amountCents = 2999,
        entitles = true,
    )

    private val monthly = BillingOption(
        priceId = "pri_03",
        sku = "monthly_03",
        amountCents = 299,
        interval = BillingOption.INTERVAL_MONTH,
        entitles = true,
    )

    private val supporter = BillingOption(
        priceId = "pri_05",
        sku = "annual_05",
        amountCents = 499,
    )

    @Test
    fun thePriceAlreadyBilledCannotBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly),
                currentSku = "annual_30",
                onSelect = {},
            )
        )

        onNodeWithText("Subscribed").assertIsNotEnabled()
        assertNotShowing("Subscribe for \$29.99/year")
    }

    @Test
    fun aSubscriptionThatStoppedBillingCanBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly),
                currentSku = "annual_30",
                sellsCurrentSku = true,
                onSelect = {},
            )
        )

        onNodeWithText("Subscribe for \$29.99/year").assertIsEnabled()
    }

    @Test
    fun aCloudSubscriberSeesTheirPlanMarkedInsteadOfARecommendation() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly, supporter),
                currentSku = "monthly_03",
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun aSupporterSeesTheirTierMarkedAndNoRecommendation() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly, supporter),
                currentSku = "annual_05",
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
        onNodeWithText("Subscribed").assertIsNotEnabled()
    }

    @Test
    fun aSupporterOnARetiredTierIsStillMarkedAsCurrent() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly, supporter),
                currentSku = "annual_07",
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun aKnownSupporterIsMarkedBeforeTheStoreAnswers() = runComposeUiTest {
        showPaywall(
            StoreBilling(loadingPrices = true, onSelect = {}),
            mode = PricingMode.BOTH,
            subscribedSku = "annual_05",
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun aKnownCloudSubscriberIsMarkedBeforeTheStoreAnswers() = runComposeUiTest {
        showPaywall(
            StoreBilling(loadingPrices = true, onSelect = {}),
            mode = PricingMode.BOTH,
            subscribedSku = "annual_30",
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun aSubscriptionTheStoreCannotSeeIsStillMarked() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            subscribedSku = "annual_30",
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun withoutASubscriptionCloudIsRecommended() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
        )

        onNodeWithText("RECOMMENDED").assertExists()
        assertNotShowing("CURRENT SUBSCRIPTION")
        assertNotShowing("Subscribed")
    }

    @Test
    fun aCardBilledAndroidSupporterTierUnlocksTheOtherPlatforms() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFeatures = SupporterFeatures.ANDROID_CARD,
        )

        assertShowing("Unlock Tasks.org for desktop and iOS")
        assertNotShowing("Unlock Tasks.org for Android")
        assertShowing("No more donation reminders")
        assertShowing("Support open source software")
        assertNotShowing("Custom themes and icons")
    }

    @Test
    fun fDroidUsersAreNotSteeredTowardsCloud() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFeatures = SupporterFeatures.ANDROID_CARD,
        )

        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun fDroidLeadsWithTheSupporterTier() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFeatures = SupporterFeatures.ANDROID_CARD,
        )

        assertTrue(
            "expected the supporter card above the cloud card",
            onNodeWithText("Tasks.org Supporter").getBoundsInRoot().top <
                    onNodeWithText("Tasks.org Cloud").getBoundsInRoot().top,
        )
    }

    @Test
    fun anFDroidCloudSubscriberStillSeesTheirPlanMarked() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), currentSku = "annual_30", onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFeatures = SupporterFeatures.ANDROID_CARD,
        )

        onNodeWithText("CURRENT SUBSCRIPTION").assertExists()
    }

    @Test
    fun aDesktopSupporterTierListsWhatTheDesktopPaywallLocks() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFeatures = SupporterFeatures.DESKTOP,
        )

        assertShowing("Unlock Tasks.org for Android, iOS, and desktop")
        assertShowing("Custom themes and icons")
        assertShowing("Sync with Google Tasks, Microsoft To Do, CalDAV, and EteSync")
        assertShowing("Support open source software")
        assertNotShowing("No more donation reminders")
    }

    @Test
    fun aStoreBilledSupporterTierListsInAppFeatures() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
        )

        assertShowing("Custom themes and icons")
        assertNotShowing("Unlock Tasks.org for Android, iOS, and desktop")
    }

    @Test
    fun aSignedInSubscribersButtonLinesUpWithTheSupporterCardsButton() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = catalog(annual, monthly, supporter),
                currentSku = "annual_30",
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
            showSignIn = false,
            width = 1000.dp,
        )

        assertEquals(
            onNodeWithText("Change to \$4.99/year").getBoundsInRoot().bottom,
            onNodeWithText("Subscribed").getBoundsInRoot().bottom,
        )
    }

    @Test
    fun aPaywallOpenedByAFeatureLeadsWithTheSupporterTier() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFirst = true,
        )

        assertTrue(
            "expected the supporter card above the cloud card",
            onNodeWithText("Tasks.org Supporter").getBoundsInRoot().top <
                    onNodeWithText("Tasks.org Cloud").getBoundsInRoot().top,
        )
        assertNotShowing("RECOMMENDED")
    }

    @Test
    fun onAWideScreenTheSupporterTierLeadsFromTheLeft() = runComposeUiTest {
        showPaywall(
            StoreBilling(catalog = catalog(annual, monthly, supporter), onSelect = {}),
            mode = PricingMode.BOTH,
            supporterFirst = true,
            width = 1000.dp,
        )

        assertTrue(
            "expected the supporter card left of the cloud card",
            onNodeWithText("Tasks.org Supporter").getBoundsInRoot().left <
                    onNodeWithText("Tasks.org Cloud").getBoundsInRoot().left,
        )
    }

    private fun catalog(vararg prices: BillingOption) = BillingCatalog.from(prices.toList())
}
