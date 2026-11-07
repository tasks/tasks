package org.tasks.compose.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.LinkResult
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.billing.StatusResult
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest
import org.tasks.billing.UnlockSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@OptIn(ExperimentalTestApi::class)
class DesktopLinkRestoreTest {

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
        var polls = 0
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

    @Test
    fun anExpiredLinkIsReplacedBeforeItIsScanned() = runComposeUiTest {
        var links = 0
        var polls = 0
        setContent {
            RestorePurchasesScreen(
                onBack = {},
                sources = listOf(UnlockSource.GOOGLE_PLAY, UnlockSource.STRIPE),
                stripe = stripeCallbacks(),
                desktopLink = DesktopLinkCallbacks(
                    onCreateLink = {
                        links++
                        LinkResult(
                            code = "link-code-$links",
                            expiresAt = currentTimeMillis() / 1000 + if (links == 1) -1 else 600,
                        )
                    },
                    onPollStatus = { code ->
                        polls++
                        if (code == "link-code-2") {
                            StatusResult(status = "confirmed", jwt = "jwt", refreshToken = "refresh")
                        } else {
                            StatusResult(status = "pending")
                        }
                    },
                    onLinkSuccess = { _, _, _, _ -> StoreResult.STORED },
                ),
                onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
                onOpenSponsorPage = {},
            )
        }
        awaitText("Google Play subscription")
        onNodeWithText("Google Play subscription").performClick()
        waitUntil(timeoutMillis = 60_000) {
            onAllNodesWithText("Desktop linked successfully!").fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(2, links)
        assertEquals(1, polls)
    }

    @Test
    fun aClockThatCallsEveryLinkExpiredStopsAskingForNewOnes() = runComposeUiTest {
        var links = 0
        setContent {
            RestorePurchasesScreen(
                onBack = {},
                sources = listOf(UnlockSource.GOOGLE_PLAY, UnlockSource.STRIPE),
                stripe = stripeCallbacks(),
                desktopLink = DesktopLinkCallbacks(
                    onCreateLink = {
                        links++
                        LinkResult(
                            code = "link-code-$links",
                            expiresAt = currentTimeMillis() / 1000 - 1,
                        )
                    },
                    onPollStatus = { StatusResult(status = "pending") },
                    onLinkSuccess = { _, _, _, _ -> StoreResult.STORED },
                ),
                onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
                onOpenSponsorPage = {},
            )
        }
        awaitText("Google Play subscription")
        onNodeWithText("Google Play subscription").performClick()

        waitUntil(timeoutMillis = 30_000) {
            onAllNodesWithText("Failed to create link. Please check your internet connection and try again.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithText("Retry").assertExists()
        assertEquals(3, links)
    }

    @Test
    fun aLinkThatCouldNotBeCreatedIsNotStillOnScreenWhenTheTabIsOpenedAgain() = runComposeUiTest {
        var links = 0
        setContent {
            RestorePurchasesScreen(
                onBack = {},
                sources = listOf(UnlockSource.GOOGLE_PLAY, UnlockSource.STRIPE),
                stripe = stripeCallbacks(),
                desktopLink = DesktopLinkCallbacks(
                    onCreateLink = {
                        links++
                        if (links == 1) {
                            null
                        } else {
                            LinkResult(
                                code = "link-code",
                                expiresAt = currentTimeMillis() / 1000 + 600,
                            )
                        }
                    },
                    onPollStatus = { StatusResult(status = "pending") },
                    onLinkSuccess = { _, _, _, _ -> StoreResult.STORED },
                ),
                onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
                onOpenSponsorPage = {},
            )
        }
        awaitText("Google Play subscription")
        onNodeWithText("Google Play subscription").performClick()
        awaitText("Failed to create link. Please check your internet connection and try again.")

        onNodeWithContentDescription("Back").performClick()
        awaitText("Google Play subscription")
        onNodeWithText("Google Play subscription").performClick()

        awaitText("Scan the QR code in the Tasks.org Android app")
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
