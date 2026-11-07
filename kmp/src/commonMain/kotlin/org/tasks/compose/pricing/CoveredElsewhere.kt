package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.tasks.billing.UnlockSource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.subscription_active_card
import tasks.kmp.generated.resources.subscription_active_elsewhere
import tasks.kmp.generated.resources.subscription_active_github
import tasks.kmp.generated.resources.subscription_active_google_play

@Composable
fun coveredElsewhereMessage(source: UnlockSource?): String = stringResource(
    when (source) {
        UnlockSource.GOOGLE_PLAY -> Res.string.subscription_active_google_play
        UnlockSource.GITHUB -> Res.string.subscription_active_github
        UnlockSource.STRIPE -> Res.string.subscription_active_card
        null -> Res.string.subscription_active_elsewhere
    }
)
