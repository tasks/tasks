package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class SubscriptionOnboardingStep { WELCOME, CREATE_LIST }

internal val OnboardingContentWidth = 480.dp

@Composable
fun SubscriptionOnboardingScreen(
    step: SubscriptionOnboardingStep,
    showConfetti: Boolean,
    onSignIn: () -> Unit,
    onCreateList: () -> Unit,
    onBack: () -> Unit,
) {
    // TODO: allow exiting flow with a confirmation dialog
    PlatformBackHandler(enabled = true, onBack = {})

    when (step) {
        SubscriptionOnboardingStep.WELCOME -> WelcomeStep(
            showConfetti = showConfetti,
            onSignIn = onSignIn,
            onClose = onBack,
        )
        SubscriptionOnboardingStep.CREATE_LIST -> CreateListStep(
            onCreateList = onCreateList,
        )
    }
}
