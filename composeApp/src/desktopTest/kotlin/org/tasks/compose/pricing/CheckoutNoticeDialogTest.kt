package org.tasks.compose.pricing

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.tasks.viewmodel.StripeCheckoutViewModel.Notice

@OptIn(ExperimentalTestApi::class)
class CheckoutNoticeDialogTest {

    @Test
    fun theAccountHoldingTheirSubscriptionIsNamed() = runComposeUiTest {
        showNotice(Notice.AccountInUse("a***@example.com"))

        awaitText("Your subscription is already linked to a***@example.com.")
    }

    @Test
    fun anAccountWithNoAddressToMaskIsStillExplained() = runComposeUiTest {
        showNotice(Notice.AccountInUse(""))

        awaitText("Your subscription is already linked to another account.")
    }

    private fun ComposeUiTest.showNotice(notice: Notice) {
        setContent { CheckoutNoticeDialog(notice = notice, onDismiss = {}) }
    }

    private fun ComposeUiTest.awaitText(text: String) =
        waitUntil { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
}
