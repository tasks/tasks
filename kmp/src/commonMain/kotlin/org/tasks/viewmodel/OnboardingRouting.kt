package org.tasks.viewmodel

enum class OnboardingScreen {
    CLOUD_ONBOARDING,
    WELCOME,
    HOME,
}

data class OnboardingState(
    val wasInOnboarding: Boolean = false,
    val wasInCloudOnboarding: Boolean = false,
)

sealed interface OnboardingNavigation {
    data class Push(val screen: OnboardingScreen) : OnboardingNavigation
    data class ClearBackStack(val screen: OnboardingScreen) : OnboardingNavigation
}

data class OnboardingRouting(
    val state: OnboardingState,
    val navigation: OnboardingNavigation? = null,
    val logOnboardingComplete: Boolean = false,
    val ready: Boolean? = null,
)

fun routeOnboarding(
    state: OnboardingState,
    hasAccount: Boolean?,
    needsCloudOnboarding: Boolean?,
    isImporting: Boolean,
    isAddingAccount: Boolean = false,
): OnboardingRouting {
    if (needsCloudOnboarding == null) {
        return OnboardingRouting(state)
    }
    if (needsCloudOnboarding) {
        return if (!state.wasInCloudOnboarding) {
            OnboardingRouting(
                state = state.copy(wasInCloudOnboarding = true),
                navigation = OnboardingNavigation.Push(OnboardingScreen.CLOUD_ONBOARDING),
                ready = true,
            )
        } else {
            OnboardingRouting(state, ready = true)
        }
    }
    if (state.wasInCloudOnboarding) {
        if (hasAccount == null) {
            return OnboardingRouting(state)
        }
        val screen = if (hasAccount) OnboardingScreen.HOME else OnboardingScreen.WELCOME
        return OnboardingRouting(
            state = state.copy(
                wasInCloudOnboarding = false,
                wasInOnboarding = !hasAccount,
            ),
            navigation = OnboardingNavigation.ClearBackStack(screen),
            logOnboardingComplete = hasAccount,
            ready = true,
        )
    }
    return when (hasAccount) {
        false -> when {
            isAddingAccount -> OnboardingRouting(state, ready = true)
            !state.wasInOnboarding -> OnboardingRouting(
                state = state.copy(wasInOnboarding = true),
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.WELCOME),
                ready = true,
            )
            else -> OnboardingRouting(state, ready = true)
        }
        true -> when {
            isImporting -> OnboardingRouting(state)
            state.wasInOnboarding -> OnboardingRouting(
                state = state.copy(wasInOnboarding = false),
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.HOME),
                logOnboardingComplete = true,
                ready = true,
            )
            else -> OnboardingRouting(state, ready = true)
        }
        null -> OnboardingRouting(state, ready = false)
    }
}
