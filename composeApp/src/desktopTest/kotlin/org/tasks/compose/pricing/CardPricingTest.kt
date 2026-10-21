package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.StripeCatalog
import org.tasks.billing.StripePrice

@OptIn(ExperimentalTestApi::class)
class CardPricingTest {

    private val annual = StripePrice(
        priceId = "pri_cloud_year",
        sku = "cloud_annual",
        amountCents = 2999,
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
    fun theGooglePlayCtaIsLeftToBuildsWithoutACardStore() = runComposeUiTest {
        showCloudCard(store = null)

        assertShowing("Subscribe on Google Play")
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
}
