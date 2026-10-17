package org.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.compose.accounts.Platform

class PricingNavigationTest {
    @Test
    fun payingForCaldavOpensTheCaldavForm() {
        assertEquals(
            AfterPaywall.Open(CaldavSignInDestination),
            afterPaywall(Platform.CALDAV.name),
        )
    }

    @Test
    fun payingForEtebaseOpensTheEtebaseForm() {
        assertEquals(
            AfterPaywall.Open(EtebaseSignInDestination),
            afterPaywall(Platform.ETEBASE.name),
        )
    }

    @Test
    fun payingForMicrosoftHandsTheSignInBackToTheAddAccountScreen() {
        assertEquals(
            AfterPaywall.SignIn(Platform.MICROSOFT),
            afterPaywall(Platform.MICROSOFT.name),
        )
    }

    @Test
    fun payingForGoogleTasksHandsTheSignInBackToTheAddAccountScreen() {
        assertEquals(
            AfterPaywall.SignIn(Platform.GOOGLE_TASKS),
            afterPaywall(Platform.GOOGLE_TASKS.name),
        )
    }

    @Test
    fun payingFromSettingsJustLeavesThePaywall() {
        assertEquals(AfterPaywall.None, afterPaywall("settings"))
    }
}
