package org.tasks.compose

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

fun NavController.navigateClearingBackStack(route: Any) =
    navigate(route) { popUpTo(0) { inclusive = true } }

fun NavController.popBackStackIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) {
        popBackStack()
    }
}
