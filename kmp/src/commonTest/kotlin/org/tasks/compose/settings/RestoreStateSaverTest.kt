package org.tasks.compose.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.tasks.billing.UnlockSource

class RestoreStateSaverTest {

    @Test
    fun everySelectableSourceComesBackAsItself() {
        UnlockSource.entries.forEach {
            assertEquals(it, roundTrip(UnlockSourceSaver, it))
        }
    }

    @Test
    fun aSourceThisVersionNoLongerKnowsComesBackAsNoSelection() {
        assertNull(UnlockSourceSaver.restore("\"CARRIER_BILLING\""))
    }

    @Test
    fun aLinkComesBackWithTheCodeItWasShowing() {
        val showQr = DesktopProState.ShowQr(code = "link-code", expiresAt = 1762000000L)

        assertEquals(showQr, roundTrip(DesktopProStateSaver, showQr))
    }

    @Test
    fun aFinishedLinkComesBackFinished() {
        assertEquals(DesktopProState.Success, roundTrip(DesktopProStateSaver, DesktopProState.Success))
    }

    @Test
    fun aFailedLinkComesBackWithTheReasonItFailed() {
        LinkFailure.entries.forEach {
            val error = DesktopProState.Error(it)

            assertEquals(error, roundTrip(DesktopProStateSaver, error))
        }
    }

    @Test
    fun anUnreadableLinkAsksForAFreshOne() {
        assertEquals(DesktopProState.Loading, DesktopProStateSaver.restore("{"))
    }

    @Test
    fun aFinishedSponsorCheckComesBackUnchanged() {
        listOf(
            GitHubProState.Idle,
            GitHubProState.Success,
            GitHubProState.NotSponsor,
            GitHubProState.NotStored(encryptionFailed = true),
            GitHubProState.NotStored(encryptionFailed = false),
            GitHubProState.Failed,
        ).forEach { assertEquals(it, roundTrip(GitHubProStateSaver, it)) }
    }

    @Test
    fun aSponsorCheckLeftInTheBrowserComesBackAtItsStart() {
        assertEquals(
            GitHubProState.Idle,
            roundTrip(GitHubProStateSaver, GitHubProState.OpeningBrowser),
        )
    }

    @Test
    fun anUnreadableSponsorCheckComesBackAtItsStart() {
        assertEquals(GitHubProState.Idle, GitHubProStateSaver.restore("{"))
    }
}
