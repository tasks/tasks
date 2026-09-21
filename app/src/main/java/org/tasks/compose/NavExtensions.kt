package org.tasks.compose

import androidx.navigation.NavController

fun NavController.navigateClearingBackStack(route: Any) =
    navigate(route) { popUpTo(0) { inclusive = true } }
