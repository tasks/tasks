package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.billing.StripePurchase
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest

@OptIn(ExperimentalTestApi::class)
class StripeRestoreTest {

    private val sends = mutableListOf<String>()
    private var answer: StripeRestoreRequest = StripeRestoreRequest.Sent
    private var verified: StripeRestore = StripeRestore.InvalidCode
    private var kept: StripeRestoreOutcome = StripeRestoreOutcome.Restored

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
    fun aPurchaseTheDiskRefusedDoesNotBlameTheKeychain() = runComposeUiTest {
        verified = StripeRestore.Restored(
            purchase = StripePurchase(purchaseJwt = "jwt", refreshToken = "refresh"),
        )
        kept = StripeRestoreOutcome.StorageFailed(encryptionFailed = false)
        setContent { Screen() }
        sendCodeTo("buyer@example.com")

        onNodeWithText("Code").performTextInput("123456")
        onAllNodesWithText("Restore purchases").filterToOne(hasClickAction()).performClick()

        awaitText("If you continue to have trouble, contact support@tasks.org")
    }

    @Test
    fun aLockedKeychainIsNamedOnTheRestoreScreen() = runComposeUiTest {
        verified = StripeRestore.Restored(
            purchase = StripePurchase(purchaseJwt = "jwt", refreshToken = "refresh"),
        )
        kept = StripeRestoreOutcome.StorageFailed(encryptionFailed = true)
        setContent { Screen() }
        sendCodeTo("buyer@example.com")

        onNodeWithText("Code").performTextInput("123456")
        onAllNodesWithText("Restore purchases").filterToOne(hasClickAction()).performClick()

        awaitText(
            "Encryption failed. Check that Tasks.org can access your system keychain, " +
                "then try again."
        )
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
                onVerifyCode = { _, _ -> verified },
                onRestored = { _, _, _ -> kept },
                onUpgrade = {},
            ),
            onCreateLink = { null },
            onPollStatus = { null },
            onLinkSuccess = { _, _, _, _ -> StoreResult.WRITE_FAILED },
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
