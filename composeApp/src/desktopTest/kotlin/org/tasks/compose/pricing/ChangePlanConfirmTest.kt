package org.tasks.compose.pricing

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.BillingOption
import org.tasks.viewmodel.StripeCheckoutViewModel

@OptIn(ExperimentalTestApi::class)
class ChangePlanConfirmTest {

    private val annual = BillingOption(
        priceId = "pri_cloud_year",
        sku = "cloud_annual",
        amountCents = 2999,
        entitles = true,
    )

    private var confirmed = 0
    private var dismissed = 0

    @Test
    fun aChangeOutsideATrialQuotesWhatItCostsToday() = runComposeUiTest {
        showDialog(StripeCheckoutViewModel.PendingChange(price = annual))

        awaitText("Your subscription will change to \$29.99/year starting today.")
    }

    @Test
    fun aChangeInsideATrialSaysWhenTheBillArrives() = runComposeUiTest {
        showDialog(
            StripeCheckoutViewModel.PendingChange(
                price = annual,
                trialEnd = TRIAL_END,
            )
        )

        awaitText("At the end of your trial you will be billed \$29.99/year.")
    }

    @Test
    fun nothingIsShownWithNoChangeHeld() = runComposeUiTest {
        showDialog(null)

        assertTrue(
            "expected no dialog",
            onAllNodes(hasText("Change plan")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun confirmingHandsTheChangeOnAndCancellingDropsIt() = runComposeUiTest {
        showDialog(StripeCheckoutViewModel.PendingChange(price = annual))
        awaitText("Cancel")

        onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissed)
        assertEquals(0, confirmed)
    }

    private fun ComposeUiTest.showDialog(pending: StripeCheckoutViewModel.PendingChange?) {
        setContent {
            ChangePlanConfirmDialog(
                pending = pending,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
    }

    private fun ComposeUiTest.awaitText(text: String) =
        waitUntil { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    companion object {
        private const val TRIAL_END = 1_767_225_600_000L
    }
}
