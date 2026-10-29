package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.tasks.billing.UnlockSource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.github_sponsorship
import tasks.kmp.generated.resources.google_play_subscription
import tasks.kmp.generated.resources.subscription_covered_by
import tasks.kmp.generated.resources.subscription_covered_elsewhere

@Composable
fun coveredElsewhereMessage(source: UnlockSource?): String = when (source) {
    UnlockSource.GOOGLE_PLAY -> stringResource(
        Res.string.subscription_covered_by,
        stringResource(Res.string.google_play_subscription),
    )
    UnlockSource.GITHUB -> stringResource(
        Res.string.subscription_covered_by,
        stringResource(Res.string.github_sponsorship),
    )
    UnlockSource.STRIPE,
    null -> stringResource(Res.string.subscription_covered_elsewhere)
}
