package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest

@OptIn(ExperimentalTestApi::class)
class StripeRestoreTest {

    private val sends = mutableListOf<String>()
    private var answer: StripeRestoreRequest = StripeRestoreRequest.Sent

    @Test
    fun aRefusedResendLeavesThemTheCodeTheyAreAlreadyHolding() = runComposeUiTest {
        setContent { Screen() }
        sendCodeTo("buyer@example.com")
        awaitText("If you paid with buyer@example.com, you'll receive a code shortly.")

        answer = StripeRestoreRequest.RateLimited
        onNodeWithText("Send code").performClick()
        awaitText("Too many attempts. Please wait a few minutes and try again.")

        assertShowing("Code")
        assertEquals(listOf("buyer@example.com", "buyer@example.com"), sends)
    }

    @Test
    fun aFirstSendThatWasRefusedGoesBackToTheAddress() = runComposeUiTest {
        setContent { Screen() }

        answer = StripeRestoreRequest.RateLimited
        sendCodeTo("buyer@example.com")
        awaitText("Too many attempts. Please wait a few minutes and try again.")

        assertShowing("Enter the email address you paid with")
    }

    private fun ComposeUiTest.sendCodeTo(address: String) {
        awaitText("Card subscription")
        onNodeWithText("Card subscription").performClick()
        awaitText("Enter the email address you paid with")
        onNodeWithText("Email").performTextInput(address)
        onNodeWithText("Send code").performClick()
    }

    @Composable
    private fun Screen() {
        RestorePurchasesScreen(
            onBack = {},
            stripe = StripeRestoreCallbacks(
                onRequestCode = { email ->
                    sends += email
                    answer
                },
                onVerifyCode = { _, _ -> StripeRestore.InvalidCode },
                onRestored = { _, _ -> StripeRestoreOutcome.Restored },
                onUpgrade = {},
            ),
            onCreateLink = { null },
            onPollStatus = { null },
            onLinkSuccess = { _, _, _, _ -> false },
            onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
            onOpenSponsorPage = {},
        )
    }

    private fun ComposeUiTest.awaitText(text: String) =
        waitUntil { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.assertShowing(text: String) = assertTrue(
        "expected to be showing \"$text\"",
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty(),
    )
}
