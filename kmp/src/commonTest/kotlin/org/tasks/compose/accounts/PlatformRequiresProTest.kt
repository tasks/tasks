package org.tasks.compose.accounts

import org.tasks.PlatformConfiguration
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformRequiresProTest {

    private val android = PlatformConfiguration(
        isAndroid = true,
        supportsCaldav = true,
        supportsEteSync = true,
        supportsOpenTasks = true,
        supportsGoogleTasks = true,
        supportsMicrosoft = true,
        thirdPartySyncRequiresPro = false,
    )

    private val desktop = android.copy(isAndroid = false, thirdPartySyncRequiresPro = true)

    @Test
    fun caldavRequiresProWhereThirdPartySyncDoesNot() {
        assertTrue(Platform.CALDAV.requiresPro(android))
        assertTrue(Platform.ETEBASE.requiresPro(android))
        assertTrue(Platform.DAVX5.requiresPro(android))
    }

    @Test
    fun caldavRequiresProWhereThirdPartySyncDoes() {
        assertTrue(Platform.CALDAV.requiresPro(desktop))
        assertTrue(Platform.ETEBASE.requiresPro(desktop))
        assertTrue(Platform.DAVX5.requiresPro(desktop))
    }

    @Test
    fun googleAndMicrosoftFollowTheThirdPartySyncSetting() {
        assertFalse(Platform.GOOGLE_TASKS.requiresPro(android))
        assertFalse(Platform.MICROSOFT.requiresPro(android))
        assertTrue(Platform.GOOGLE_TASKS.requiresPro(desktop))
        assertTrue(Platform.MICROSOFT.requiresPro(desktop))
    }

    @Test
    fun tasksOrgIsGatedOnItsOwnSubscriptionNotPro() {
        assertFalse(Platform.TASKS_ORG.requiresPro(android))
        assertFalse(Platform.TASKS_ORG.requiresPro(desktop))
    }
}
