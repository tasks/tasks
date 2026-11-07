package org.tasks.compose.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.LinkResult
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest
import org.tasks.billing.UnlockSource

@OptIn(ExperimentalTestApi::class)
class DesktopLinkRestoreTest {

    private var polls = 0

    @Test
    fun aPlatformWithNoWayToLinkDoesNotOfferTheQrFlow() = runComposeUiTest {
        setContent {
            RestorePurchasesScreen(
                onBack = {},
                sources = UnlockSource.entries,
                stripe = stripeCallbacks(),
                onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
                onOpenSponsorPage = {},
            )
        }
        awaitText("Card subscription")

        assertEquals(
            0,
            onAllNodesWithText("Google Play subscription").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aLinkServerThatKeepsFailingStopsPollingAndOffersToStartOver() = runComposeUiTest {
        setContent {
            RestorePurchasesScreen(
                onBack = {},
                sources = listOf(UnlockSource.GOOGLE_PLAY, UnlockSource.STRIPE),
                stripe = stripeCallbacks(),
                desktopLink = DesktopLinkCallbacks(
                    onCreateLink = { LinkResult(code = "link-code", expiresAt = 0L) },
                    onPollStatus = {
                        polls++
                        null
                    },
                    onLinkSuccess = { _, _, _, _ -> StoreResult.WRITE_FAILED },
                ),
                onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
                onOpenSponsorPage = {},
            )
        }
        awaitText("Google Play subscription")
        onNodeWithText("Google Play subscription").performClick()
        awaitText("Scan the QR code in the Tasks.org Android app")

        waitUntil(timeoutMillis = 300_000) {
            onAllNodesWithText("Failed to create link. Please check your internet connection and try again.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithText("Retry").assertExists()
        assertEquals(5, polls)
    }

    private fun stripeCallbacks() = StripeRestoreCallbacks(
        onRequestCode = { StripeRestoreRequest.Sent },
        onVerifyCode = { _, _ -> StripeRestore.InvalidCode },
        onRestored = { _, _, _ -> StripeRestoreOutcome.Restored },
        onUpgrade = {},
    )

    private fun ComposeUiTest.awaitText(text: String) =
        waitUntil { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
}
