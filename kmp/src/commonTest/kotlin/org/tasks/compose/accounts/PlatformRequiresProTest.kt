package org.tasks.compose.accounts

import org.tasks.PlatformConfiguration
import org.tasks.billing.BillingProvider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformRequiresProTest {

    private val android = PlatformConfiguration(
        billingProvider = BillingProvider.GOOGLE_PLAY,
        isAndroid = true,
        supportsCaldav = true,
        supportsEteSync = true,
        supportsOpenTasks = true,
        supportsGoogleTasks = true,
        supportsMicrosoft = true,
        googleTasksRequiresPro = false,
    )

    private val desktop = android.copy(isAndroid = false, googleTasksRequiresPro = true)

    @Test
    fun caldavRequiresProWhereGoogleTasksDoesNot() {
        assertTrue(Platform.CALDAV.requiresPro(android))
        assertTrue(Platform.ETEBASE.requiresPro(android))
        assertTrue(Platform.DAVX5.requiresPro(android))
    }

    @Test
    fun caldavRequiresProWhereGoogleTasksDoes() {
        assertTrue(Platform.CALDAV.requiresPro(desktop))
        assertTrue(Platform.ETEBASE.requiresPro(desktop))
        assertTrue(Platform.DAVX5.requiresPro(desktop))
    }

    @Test
    fun googleTasksFollowsItsSetting() {
        assertFalse(Platform.GOOGLE_TASKS.requiresPro(android))
        assertTrue(Platform.GOOGLE_TASKS.requiresPro(desktop))
    }

    @Test
    fun microsoftToDoIsFreeEverywhere() {
        assertFalse(Platform.MICROSOFT.requiresPro(android))
        assertFalse(Platform.MICROSOFT.requiresPro(desktop))
    }

    @Test
    fun tasksOrgIsGatedOnItsOwnSubscriptionNotPro() {
        assertFalse(Platform.TASKS_ORG.requiresPro(android))
        assertFalse(Platform.TASKS_ORG.requiresPro(desktop))
    }
}
