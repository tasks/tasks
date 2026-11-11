package org.tasks.compose.pricing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val ladder = listOf(1, 2, 3, 5, 10, 20).map { dollars ->
        BillingOption(
            priceId = "pri_$dollars",
            sku = "annual_" + dollars.toString().padStart(2, '0'),
            amountCents = dollars * 100 - 1,
        )
    }

    private fun ComposeUiTest.showLadder(suggestedPrice: Int? = null) = showPaywall(
        store = StoreBilling(
            catalog = BillingCatalog.from(ladder),
            suggestedPrice = suggestedPrice,
            onSelect = {},
        ),
        mode = PricingMode.NYP_ONLY,
    )

    @Test
    fun thePresetsOpenOnTheDefaultTier() = runComposeUiTest {
        showLadder()

        onNodeWithText("\$9.99").assertIsSelected()
        assertShowing("Subscribe for \$9.99/year")
        assertTrue("expected the presets instead of a slider", sliders().isEmpty())
    }

    @Test
    fun aPresetPicksItsTier() = runComposeUiTest {
        showLadder()

        onNodeWithText("\$4.99").performClick()

        assertShowing("Subscribe for \$4.99/year")
    }

    @Test
    fun customSwapsThePresetsForTheSlider() = runComposeUiTest {
        showLadder()

        onNodeWithText("Custom").performClick()

        assertTrue("expected the slider", sliders().isNotEmpty())
        assertTrue(
            "expected the presets to make way for the slider",
            onAllNodes(hasText("\$4.99")).fetchSemanticsNodes().isEmpty(),
        )
        assertShowing("Subscribe for \$9.99/year")
    }

    @Test
    fun aTierWithoutAPresetOpensOnTheSlider() = runComposeUiTest {
        showLadder(suggestedPrice = 2)

        assertTrue("expected the slider", sliders().isNotEmpty())
        assertShowing("Subscribe for \$1.99/year")
    }

    private val cloud = BillingOption(
        priceId = "pri_30",
        sku = "annual_30",
        amountCents = 2999,
        entitles = true,
    )

    private fun ComposeUiTest.showSupporter(currentSku: String?, suggestedPrice: Int?) = showPaywall(
        store = StoreBilling(
            catalog = BillingCatalog.from(ladder + cloud),
            currentSku = currentSku,
            suggestedPrice = suggestedPrice,
            onSelect = {},
        ),
        mode = PricingMode.BOTH,
    )

    @Test
    fun aSupporterPayingMoreIsUpgrading() = runComposeUiTest {
        showSupporter(currentSku = "annual_02", suggestedPrice = 2)

        onNodeWithText("Custom").assertDoesNotExist()
        slideTo(4f)

        assertShowing("Upgrade to \$9.99/year")
    }

    @Test
    fun aSupporterPayingLessIsChanging() = runComposeUiTest {
        showSupporter(currentSku = "annual_10", suggestedPrice = 10)

        onNodeWithText("\$4.99").performScrollTo().performClick()

        assertShowing("Change to \$4.99/year")
    }

    @Test
    fun aSupporterMovingToCloudIsUpgrading() = runComposeUiTest {
        showSupporter(currentSku = "annual_05", suggestedPrice = 5)

        assertShowing("Upgrade to \$29.99/year")
    }

    @Test
    fun aNewCustomerIsSubscribing() = runComposeUiTest {
        showSupporter(currentSku = null, suggestedPrice = null)

        assertShowing("Subscribe for \$9.99/year")
        assertShowing("Subscribe for \$29.99/year")
    }

    @Test
    fun theSupporterPriceStartsFromTheCheapestTier() = runComposeUiTest {
        showLadder()

        assertShowing("From \$0.99")
    }

    @Test
    fun choosingATierShowsItsPrice() = runComposeUiTest {
        showLadder()

        onNodeWithText("\$4.99").performClick()

        assertNotShowing("From ")
        assertEquals(2, onAllNodes(hasText("\$4.99")).fetchSemanticsNodes().size)
    }

    @Test
    fun aSupporterSeesWhatTheyPay() = runComposeUiTest {
        showSupporter(currentSku = "annual_02", suggestedPrice = 2)

        assertNotShowing("From ")
        assertShowing("\$1.99")
    }

    private fun ComposeUiTest.sliders() =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).fetchSemanticsNodes()

    private fun ComposeUiTest.slideTo(position: Float) {
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(position) }
        waitForIdle()
    }

    private fun ComposeUiTest.restoreButtonY() =
        onNodeWithText("Restore purchases").fetchSemanticsNode().positionInRoot.y
}
