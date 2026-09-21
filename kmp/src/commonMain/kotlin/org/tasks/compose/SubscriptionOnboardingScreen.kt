package org.tasks.compose

import androidx.compose.runtime.Composable

enum class SubscriptionOnboardingStep { WELCOME, CREATE_LIST }

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
