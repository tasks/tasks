package com.todoroo.astrid.activity

import com.todoroo.astrid.activity.MainActivityViewModel.OnboardingNavigation
import com.todoroo.astrid.activity.MainActivityViewModel.OnboardingRouting
import com.todoroo.astrid.activity.MainActivityViewModel.OnboardingState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.compose.HomeDestination
import org.tasks.compose.SubscriptionOnboardingDestination
import org.tasks.compose.WelcomeDestination

class MainActivityViewModelTest {

    private fun route(
        state: OnboardingState = OnboardingState(),
        hasAccount: Boolean? = null,
        needsCloudOnboarding: Boolean? = false,
        isImporting: Boolean = false,
    ) = MainActivityViewModel.routeOnboarding(
        state = state,
        hasAccount = hasAccount,
        needsCloudOnboarding = needsCloudOnboarding,
        isImporting = isImporting,
    )

    @Test
    fun waitsWhileCloudOnboardingFlagUnknown() {
        assertEquals(
            OnboardingRouting(OnboardingState()),
            route(needsCloudOnboarding = null, hasAccount = true),
        )
    }

    @Test
    fun pushesCloudOnboardingOnce() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInCloudOnboarding = true),
                navigation = OnboardingNavigation.Push(SubscriptionOnboardingDestination),
                ready = true,
            ),
            route(needsCloudOnboarding = true),
        )
    }

    @Test
    fun doesNotRePushCloudOnboardingOnceShown() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInCloudOnboarding = true),
                ready = true,
            ),
            route(
                state = OnboardingState(wasInCloudOnboarding = true),
                needsCloudOnboarding = true,
            ),
        )
    }

    @Test
    fun waitsForAccountAfterCloudOnboarding() {
        assertEquals(
            OnboardingRouting(OnboardingState(wasInCloudOnboarding = true)),
            route(
                state = OnboardingState(wasInCloudOnboarding = true),
                needsCloudOnboarding = false,
                hasAccount = null,
            ),
        )
    }

    @Test
    fun cloudOnboardingWithAccountGoesHomeAndLogs() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(),
                navigation = OnboardingNavigation.ClearBackStack(HomeDestination),
                logOnboardingComplete = true,
                ready = true,
            ),
            route(
                state = OnboardingState(wasInCloudOnboarding = true),
                needsCloudOnboarding = false,
                hasAccount = true,
            ),
        )
    }

    @Test
    fun cloudOnboardingWithoutAccountGoesToWelcomeWithoutLogging() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInOnboarding = true),
                navigation = OnboardingNavigation.ClearBackStack(WelcomeDestination),
                logOnboardingComplete = false,
                ready = true,
            ),
            route(
                state = OnboardingState(wasInCloudOnboarding = true),
                needsCloudOnboarding = false,
                hasAccount = false,
            ),
        )
    }

    @Test
    fun routesToWelcomeWhenNoAccount() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInOnboarding = true),
                navigation = OnboardingNavigation.ClearBackStack(WelcomeDestination),
                ready = true,
            ),
            route(hasAccount = false),
        )
    }

    @Test
    fun doesNotReRouteToWelcomeOnceShown() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInOnboarding = true),
                ready = true,
            ),
            route(
                state = OnboardingState(wasInOnboarding = true),
                hasAccount = false,
            ),
        )
    }

    @Test
    fun waitsForImportToFinishBeforeLeavingOnboarding() {
        assertEquals(
            OnboardingRouting(OnboardingState(wasInOnboarding = true)),
            route(
                state = OnboardingState(wasInOnboarding = true),
                hasAccount = true,
                isImporting = true,
            ),
        )
    }

    @Test
    fun leavesOnboardingForHomeOnceAccountExists() {
        assertEquals(
            OnboardingRouting(
                state = OnboardingState(),
                navigation = OnboardingNavigation.ClearBackStack(HomeDestination),
                logOnboardingComplete = true,
                ready = true,
            ),
            route(
                state = OnboardingState(wasInOnboarding = true),
                hasAccount = true,
            ),
        )
    }

    @Test
    fun readyWithAccountAndNoPendingOnboarding() {
        assertEquals(
            OnboardingRouting(OnboardingState(), ready = true),
            route(hasAccount = true),
        )
    }

    @Test
    fun notReadyWhileAccountStateUnknown() {
        assertEquals(
            OnboardingRouting(OnboardingState(), ready = false),
            route(hasAccount = null),
        )
    }

    @Test
    fun fullCloudPurchaseFlow() {
        var state = OnboardingState()

        // 1. Purchase sets the flag -> push subscription onboarding once.
        route(state, hasAccount = false, needsCloudOnboarding = true).let {
            assertEquals(OnboardingNavigation.Push(SubscriptionOnboardingDestination), it.navigation)
            state = it.state
        }

        // 2. Flag still set, recomposition -> no duplicate push.
        route(state, hasAccount = false, needsCloudOnboarding = true).let {
            assertEquals(null, it.navigation)
            state = it.state
        }

        // 3. Onboarding completes, account now exists -> clear to home and log completion.
        route(state, hasAccount = true, needsCloudOnboarding = false).let {
            assertEquals(OnboardingNavigation.ClearBackStack(HomeDestination), it.navigation)
            assertEquals(true, it.logOnboardingComplete)
            state = it.state
        }

        // 4. Steady state -> ready, no further navigation.
        route(state, hasAccount = true, needsCloudOnboarding = false).let {
            assertEquals(null, it.navigation)
            assertEquals(false, it.logOnboardingComplete)
            assertEquals(true, it.ready)
        }
    }
}
