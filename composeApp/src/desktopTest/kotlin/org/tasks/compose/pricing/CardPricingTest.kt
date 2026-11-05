package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.StripeCatalog
import org.tasks.billing.StripePrice
import org.tasks.billing.UnlockSource
import org.tasks.themes.TestTags

@OptIn(ExperimentalTestApi::class)
class CardPricingTest {

    private val annual = StripePrice(
        priceId = "pri_cloud_year",
        sku = "cloud_annual",
        amountCents = 2999,
        entitles = true,
        trialDays = 14,
    )

    private val monthly = StripePrice(
        priceId = "pri_cloud_month",
        sku = "cloud_monthly",
        amountCents = 299,
        interval = StripePrice.INTERVAL_MONTH,
        entitles = true,
        trialDays = 14,
    )

    @Test
    fun theCtaQuotesTheCatalogPrice() = runComposeUiTest {
        showCloudCard(StoreBilling(catalog = StripeCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Subscribe for \$29.99/year")
        assertShowing("14-day free trial")
    }

    @Test
    fun aFreeWeekIsNotPromisedToSomebodyWhoHasBoughtBefore() = runComposeUiTest {
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual)),
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
        showCloudCard(store = null)

        assertShowing("Subscribe on Google Play")
    }

    @Test
    fun aCatalogStillLoadingDoesNotQuoteAPriceItHasNotFetched() = runComposeUiTest {
        showCloudCard(StoreBilling(loadingPrices = true, onSelect = {}))

        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected the price to be a placeholder", it.isNotEmpty())
        }
        assertNotShowing("\$29.99")
        assertNotShowing("\$2.50")
    }

    @Test
    fun aStoreThatCannotPriceAnythingSaysNothingRatherThanDollars() = runComposeUiTest {
        showCloudCard(StoreBilling(onSelect = {}))

        assertNotShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer once the catalog has settled", it.isEmpty())
        }
    }

    @Test
    fun buildsWithoutACardStoreKeepTheirIndicativePrice() = runComposeUiTest {
        showCloudCard(store = null)

        assertShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer where no catalog is ever fetched", it.isEmpty())
        }
    }

    @Test
    fun aSettledCatalogWithNothingToSellSaysSoInsteadOfOfferingGooglePlay() = runComposeUiTest {
        showCloudCard(StoreBilling(onSelect = {}))

        assertShowing("Billing isn't available right now. Please try again later.")
        assertTrue(
            "expected no Google Play call to action",
            onAllNodes(hasText("Subscribe on Google Play")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aPurchaseInFlightDisablesTheCta() = runComposeUiTest {
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual)),
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
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual)),
                currentSku = annual.sku,
                onSelect = {},
            )
        )

        onNodeWithText("Subscribe for \$29.99/year").assertIsNotEnabled()
    }

    @Test
    fun restoreIsOnThePaywallAndNotOnlyAfterAReinstall() = runComposeUiTest {
        var restored = false
        setContent {
            Box(modifier = Modifier.size(480.dp, 2000.dp)) {
                PricingScreen(
                    mode = PricingMode.CLOUD_ONLY,
                    onBack = {},
                    onSignIn = {},
                    onRestorePurchases = { restored = true },
                    store = StoreBilling(
                        catalog = StripeCatalog.from(listOf(annual)),
                        onSelect = {},
                    ),
                )
            }
        }

        onNodeWithText("Restore purchases").performScrollTo().performClick()

        assertTrue("expected the restore screen to be asked for", restored)
    }

    @Test
    fun theRestoreLinkDoesNotAskAgainWhatTheSignInFooterAsked() = runComposeUiTest {
        showCloudCard(StoreBilling(catalog = StripeCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Already a subscriber or invited guest?")
        assertShowing("Restore purchases")
        assertTrue(
            "expected the restore link not to repeat the question",
            onAllNodes(hasText("Already a subscriber?")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun anAccountAnotherProviderPaysForIsWarnedButStillSoldTo() = runComposeUiTest {
        var selected: StripePrice? = null
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.GOOGLE_PLAY,
                onSelect = { selected = it },
            )
        )

        assertShowing("This account is paid for by your Google Play subscription.")
        onNodeWithText("Subscribe for \$29.99/year").performScrollTo().performClick()

        assertEquals(annual.sku, selected?.sku)
    }

    @Test
    fun aCardSubscriptionOfTheirOwnIsStillTheirsToReprice() = runComposeUiTest {
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual, monthly)),
                currentSku = monthly.sku,
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("Subscribe for \$29.99/year")
        assertTrue(
            "expected no coverage warning on a reprice",
            onAllNodes(hasText("already paid for somewhere else", substring = true))
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aProviderTheAppCannotNameIsWarnedAboutInTheGeneralCase() = runComposeUiTest {
        showCloudCard(
            StoreBilling(
                catalog = StripeCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("This account is already paid for somewhere else.")
        assertShowing("Subscribe for \$29.99/year")
    }

    private fun ComposeUiTest.showCloudCard(store: StoreBilling?) {
        setContent {
            Box(modifier = Modifier.size(480.dp, 2000.dp)) {
                PricingScreen(
                    mode = PricingMode.CLOUD_ONLY,
                    onBack = {},
                    onSignIn = {},
                    onRestorePurchases = {},
                    store = store,
                )
            }
        }
    }

    private fun ComposeUiTest.assertShowing(text: String) = assertTrue(
        "expected to be showing \"$text\"",
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty(),
    )

    private fun ComposeUiTest.assertNotShowing(text: String) = assertTrue(
        "expected not to be showing \"$text\"",
        onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isEmpty(),
    )
}
