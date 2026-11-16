package org.tasks

import androidx.navigation3.runtime.NavKey
import org.tasks.compose.accounts.Platform

internal sealed interface AfterPaywall {
    data object None : AfterPaywall

    data class Open(val destination: NavKey) : AfterPaywall

    data class SignIn(val platform: Platform) : AfterPaywall
}

internal fun afterPaywall(source: String): AfterPaywall = when (Platform.fromSource(source)) {
    Platform.CALDAV -> AfterPaywall.Open(CaldavSignInDestination)
    Platform.ETEBASE -> AfterPaywall.Open(EtebaseSignInDestination)
    Platform.GOOGLE_TASKS -> AfterPaywall.SignIn(Platform.GOOGLE_TASKS)
    Platform.MICROSOFT -> AfterPaywall.SignIn(Platform.MICROSOFT)
    Platform.DAVX5,
    Platform.TASKS_ORG,
    null -> AfterPaywall.None
}
