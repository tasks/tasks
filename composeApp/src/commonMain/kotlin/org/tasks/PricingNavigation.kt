package org.tasks

import androidx.navigation3.runtime.NavKey
import org.tasks.compose.accounts.Platform

internal sealed interface AfterPaywall {
    data object None : AfterPaywall

    data class Open(val destination: NavKey) : AfterPaywall

    data class SignIn(val platform: Platform) : AfterPaywall
}

internal fun afterPaywall(source: String): AfterPaywall = when (source) {
    Platform.CALDAV.name -> AfterPaywall.Open(CaldavSignInDestination)
    Platform.ETEBASE.name -> AfterPaywall.Open(EtebaseSignInDestination)
    Platform.GOOGLE_TASKS.name -> AfterPaywall.SignIn(Platform.GOOGLE_TASKS)
    Platform.MICROSOFT.name -> AfterPaywall.SignIn(Platform.MICROSOFT)
    else -> AfterPaywall.None
}
