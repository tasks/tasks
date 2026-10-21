package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.StripeCatalog
import org.tasks.billing.StripePrice

@OptIn(ExperimentalTestApi::class)
class NameYourPricePickerTest {

    private val rungs = listOf(
        StripePrice(priceId = "pri_01", sku = "annual_01", amountCents = 99),
        StripePrice(priceId = "pri_03", sku = "annual_03", amountCents = 299),
        StripePrice(priceId = "pri_05", sku = "annual_05", amountCents = 499),
    )

    private val monthly = StripePrice(
        priceId = "pri_05m",
        sku = "monthly_05",
        amountCents = 499,
        interval = StripePrice.INTERVAL_MONTH,
    )

    @Test
    fun theCardIsTheSameHeightOnEveryRung() = runComposeUiTest {
        setContent {
            Box(modifier = Modifier.size(480.dp, 2000.dp)) {
                PricingScreen(
                    mode = PricingMode.NYP_ONLY,
                    onBack = {},
                    onSignIn = {},
                    onRestorePurchases = {},
                    store = StoreBilling(
                        catalog = StripeCatalog.from(rungs + monthly),
                        suggestedPrice = 1,
                        onSelect = {},
                    ),
                )
            }
        }
        assertShowing("Subscribe for \$0.99/year")
        val annualOnly = restoreButtonY()

        slideTo(1f)

        assertShowing("Subscribe for \$2.99/year")
        assertEquals(annualOnly, restoreButtonY(), 0f)

        slideTo(2f)

        assertShowing("\$4.99/year")
        assertShowing("\$4.99/month")
        assertEquals(annualOnly, restoreButtonY(), 0f)
    }

    private fun ComposeUiTest.slideTo(position: Float) {
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(position) }
        waitForIdle()
    }

    private fun ComposeUiTest.restoreButtonY() =
        onNodeWithText("Restore purchases").fetchSemanticsNode().positionInRoot.y

    private fun ComposeUiTest.assertShowing(text: String) = assertTrue(
        "expected to be showing \"$text\"",
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty(),
    )
}
