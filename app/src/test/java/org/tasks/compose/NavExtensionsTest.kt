package org.tasks.compose

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NavExtensionsTest {
    @Serializable
    data object Home

    @Serializable
    data object Welcome

    @Serializable
    data object AddAccount

    private val navController = NavHostController(RuntimeEnvironment.getApplication()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = Home) {
            composable<Home> {}
            composable<Welcome> {}
            composable<AddAccount> {}
        }
    }

    @Test
    fun anAddedAccountClosesTheAddAccountScreen() {
        navController.navigateClearingBackStack(Welcome)
        navController.navigate(AddAccount)
        val addAccount = navController.currentBackStackEntry!!

        navController.popBackStackIfCurrent(addAccount)

        assertEquals(listOf(true), screens().map { it.hasRoute(Welcome::class) })
    }

    @Test
    fun finishingOnboardingFirstDoesNotPopHomeOffTheBackStack() {
        navController.navigateClearingBackStack(Welcome)
        navController.navigate(AddAccount)
        val addAccount = navController.currentBackStackEntry!!
        navController.navigateClearingBackStack(Home)

        navController.popBackStackIfCurrent(addAccount)

        assertEquals(listOf(true), screens().map { it.hasRoute(Home::class) })
    }

    private fun screens(): List<NavDestination> =
        navController.currentBackStack.value.map { it.destination }.filter { it !is NavGraph }
}
