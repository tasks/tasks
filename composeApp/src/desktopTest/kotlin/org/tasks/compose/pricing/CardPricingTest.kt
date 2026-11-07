package org.tasks.compose.pricing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption
import org.tasks.billing.UnlockSource
import org.tasks.resources.getString
import org.tasks.themes.TestTags
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.upgrade_subscription_banner

@OptIn(ExperimentalTestApi::class)
class CardPricingTest {

    private val annual = BillingOption(
        priceId = "pri_cloud_year",
        sku = "cloud_annual",
        amountCents = 2999,
        entitles = true,
        trialDays = 14,
    )

    private val monthly = BillingOption(
        priceId = "pri_cloud_month",
        sku = "cloud_monthly",
        amountCents = 299,
        interval = BillingOption.INTERVAL_MONTH,
        entitles = true,
        trialDays = 14,
    )

    private val supporter = BillingOption(
        priceId = "pri_support_year",
        sku = "annual_01",
        amountCents = 100,
    )

    private val everyTier = StoreBilling(
        catalog = BillingCatalog.from(listOf(annual, monthly, supporter)),
        onSelect = {},
    )

    private val supporterBanner =
        runBlocking { getString(Res.string.upgrade_subscription_banner) }

    @Test
    fun theCtaQuotesThePriceFromTheCatalog() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Subscribe for \$29.99/year")
        assertShowing("14-day free trial")
    }

    @Test
    fun aFreeWeekIsNotPromisedToSomebodyWhoHasBoughtBefore() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                knownCustomer = true,
                onSelect = {},
            )
        )

        assertShowing("Subscribe for \$29.99/year")
        assertTrue(
            "expected no free trial promise",
            onAllNodes(hasText("14-day free trial")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun theGooglePlayCtaIsLeftToBuildsWithoutACardStore() = runComposeUiTest {
        showPaywall(store = null)

        assertShowing("Subscribe on Google Play")
    }

    @Test
    fun aCatalogStillLoadingDoesNotQuoteAPriceItHasNotFetched() = runComposeUiTest {
        showPaywall(StoreBilling(loadingPrices = true, onSelect = {}))

        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected the price to be a placeholder", it.isNotEmpty())
        }
        assertNotShowing("\$29.99")
        assertNotShowing("\$2.50")
    }

    @Test
    fun aStoreThatCannotPriceAnythingSaysNothingRatherThanDollars() = runComposeUiTest {
        showPaywall(StoreBilling(onSelect = {}))

        assertNotShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer once the catalog has settled", it.isEmpty())
        }
    }

    @Test
    fun buildsWithoutACardStoreKeepTheirIndicativePrice() = runComposeUiTest {
        showPaywall(store = null)

        assertShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer where no catalog is ever fetched", it.isEmpty())
        }
    }

    @Test
    fun aSettledCatalogWithNothingToSellSaysSoInsteadOfOfferingGooglePlay() = runComposeUiTest {
        showPaywall(StoreBilling(onSelect = {}))

        assertShowing("Billing isn't available right now. Please try again later.")
        assertTrue(
            "expected no Google Play call to action",
            onAllNodes(hasText("Subscribe on Google Play")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aPurchaseInFlightDisablesTheCta() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                busySku = annual.sku,
                onSelect = {},
            )
        )

        onNodeWithText("Annual").assertExists()
        onAllNodes(hasText("Subscribe for \$29.99/year")).fetchSemanticsNodes().let {
            assertTrue("expected the priced cta to be replaced by a spinner", it.isEmpty())
        }
    }

    @Test
    fun theCurrentPlanCannotBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                currentSku = annual.sku,
                onSelect = {},
            )
        )

        onNodeWithText("Subscribe for \$29.99/year").assertIsNotEnabled()
    }

    @Test
    fun restoreIsOnThePaywallAndNotOnlyAfterAReinstall() = runComposeUiTest {
        var restored = false
        showPaywall(
            store = StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}),
            onRestorePurchases = { restored = true },
        )

        onNodeWithText("Restore purchases").performScrollTo().performClick()

        assertTrue("expected the restore screen to be asked for", restored)
    }

    @Test
    fun theRestoreLinkDoesNotAskAgainWhatTheSignInFooterAsked() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Already a subscriber or invited guest?")
        assertShowing("Restore purchases")
        assertTrue(
            "expected the restore link not to repeat the question",
            onAllNodes(hasText("Already a subscriber?")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun anAccountAnotherProviderPaysForIsWarnedButStillSoldTo() = runComposeUiTest {
        var selected: BillingOption? = null
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.GOOGLE_PLAY,
                onSelect = { selected = it },
            )
        )

        assertShowing("You already have an active Google Play subscription.")
        onNodeWithText("Subscribe for \$29.99/year").performScrollTo().performClick()

        assertEquals(annual.sku, selected?.sku)
    }

    @Test
    fun aGitHubSponsorIsWarnedByName() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.GITHUB,
                onSelect = {},
            )
        )

        assertShowing("You already have an active GitHub sponsorship.")
    }

    @Test
    fun aCardSubscriptionOfTheirOwnIsStillTheirsToReprice() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, monthly)),
                currentSku = monthly.sku,
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("Subscribe for \$29.99/year")
        assertTrue(
            "expected no coverage warning on a reprice",
            onAllNodes(hasText("active subscription somewhere else", substring = true))
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aSupporterSubscriptionDoesNotSuppressTheCoverageWarning() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                currentSku = supporter.sku,
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            )
        )

        assertShowing("You already have an active subscription paid by card.")
    }

    @Test
    fun aCloudPlanTheStoreNoLongerSellsStillSuppressesTheWarning() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                currentSku = "monthly_499",
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            )
        )

        assertNotShowing("You already have an active subscription paid by card.")
    }

    @Test
    fun aProviderTheAppCannotNameIsWarnedAboutInTheGeneralCase() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("You already have an active subscription somewhere else.")
        assertShowing("Subscribe for \$29.99/year")
    }

    @Test
    fun aSupporterTierIsWarnedAboutAPaymentElsewhereToo() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        assertEquals(
            2,
            onAllNodes(hasText("You already have an active subscription paid by card."))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSupporterRepricingTheirOwnPlanIsOnlyWarnedOnCloud() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                currentSku = supporter.sku,
                coveredElsewhere = true,
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        assertEquals(
            1,
            onAllNodes(hasText("You already have an active subscription somewhere else."))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSupporterIsAskedToUpgradeWhenCloudIsTheOnlyThingOnOffer() = runComposeUiTest {
        showPaywall(
            store = everyTier,
            mode = PricingMode.CLOUD_ONLY,
            hasSupporterSubscription = true,
        )

        assertShowing(supporterBanner)
    }

    @Test
    fun aSupporterBrowsingEveryTierIsNotToldToUpgrade() = runComposeUiTest {
        showPaywall(
            store = everyTier,
            mode = PricingMode.BOTH,
            hasSupporterSubscription = true,
        )

        assertShowing(CLOUD_TIER)
        assertShowing(SUPPORTER_TIER)
        assertNotShowing(supporterBanner)
    }

    @Test
    fun aSupporterNamingTheirOwnPriceIsNotToldToUpgrade() = runComposeUiTest {
        showPaywall(
            store = everyTier,
            mode = PricingMode.NYP_ONLY,
            hasSupporterSubscription = true,
        )

        assertShowing(SUPPORTER_TIER)
        assertNotShowing(supporterBanner)
    }

    @Test
    fun somebodyWhoPaysUsNothingIsNotThankedForTheirSupport() = runComposeUiTest {
        showPaywall(
            store = everyTier,
            mode = PricingMode.CLOUD_ONLY,
            hasSupporterSubscription = false,
        )

        assertShowing(CLOUD_TIER)
        assertNotShowing(supporterBanner)
    }

    private companion object {
        const val CLOUD_TIER = "Tasks.org Cloud"
        const val SUPPORTER_TIER = "Tasks.org Supporter"
    }
}
