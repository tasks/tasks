package org.tasks.compose.pricing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption

@OptIn(ExperimentalTestApi::class)
class NameYourPricePickerTest {

    private val rungs = listOf(
        BillingOption(priceId = "pri_01", sku = "annual_01", amountCents = 99),
        BillingOption(priceId = "pri_03", sku = "annual_03", amountCents = 299),
        BillingOption(priceId = "pri_05", sku = "annual_05", amountCents = 499),
    )

    private val monthly = BillingOption(
        priceId = "pri_05m",
        sku = "monthly_05",
        amountCents = 499,
        interval = BillingOption.INTERVAL_MONTH,
    )

    @Test
    fun theCardIsTheSameHeightOnEveryRung() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(rungs + monthly),
                suggestedPrice = 1,
                onSelect = {},
            ),
            mode = PricingMode.NYP_ONLY,
        )

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
}
