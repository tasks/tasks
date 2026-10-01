package org.tasks.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingRoutingTest {

    private fun route(
        state: OnboardingState = OnboardingState(),
        hasAccount: Boolean? = null,
        needsCloudOnboarding: Boolean? = false,
        isImporting: Boolean = false,
        isAddingAccount: Boolean = false,
    ) = routeOnboarding(
        state = state,
        hasAccount = hasAccount,
        needsCloudOnboarding = needsCloudOnboarding,
        isImporting = isImporting,
        isAddingAccount = isAddingAccount,
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
                navigation = OnboardingNavigation.Push(OnboardingScreen.CLOUD_ONBOARDING),
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
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.HOME),
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
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.WELCOME),
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
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.WELCOME),
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
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.HOME),
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
            assertEquals(OnboardingNavigation.Push(OnboardingScreen.CLOUD_ONBOARDING), it.navigation)
            state = it.state
        }

        // 2. Flag still set, recomposition -> no duplicate push.
        route(state, hasAccount = false, needsCloudOnboarding = true).let {
            assertEquals(null, it.navigation)
            state = it.state
        }

        // 3. Onboarding completes, account now exists -> clear to home and log completion.
        route(state, hasAccount = true, needsCloudOnboarding = false).let {
            assertEquals(OnboardingNavigation.ClearBackStack(OnboardingScreen.HOME), it.navigation)
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

    @Test
    fun losingTheLastAccountWhileAddingOneLeavesTheStackAlone() {
        assertEquals(
            OnboardingRouting(OnboardingState(), ready = true),
            route(hasAccount = false, isAddingAccount = true),
        )
    }

    @Test
    fun theDecisionIsRevisitedOnceTheyLeaveTheAddAccountFlow() {
        val deferred = route(hasAccount = false, isAddingAccount = true)

        assertEquals(
            OnboardingRouting(
                state = OnboardingState(wasInOnboarding = true),
                navigation = OnboardingNavigation.ClearBackStack(OnboardingScreen.WELCOME),
                ready = true,
            ),
            route(state = deferred.state, hasAccount = false),
        )
    }

    @Test
    fun alreadyBeingInOnboardingIsNotDisturbedByAddingAnAccount() {
        assertEquals(
            OnboardingRouting(OnboardingState(wasInOnboarding = true), ready = true),
            route(
                state = OnboardingState(wasInOnboarding = true),
                hasAccount = false,
                isAddingAccount = true,
            ),
        )
    }

    @Test
    fun addingASecondAccountNeverReachesTheExemption() {
        assertEquals(
            OnboardingRouting(OnboardingState(), ready = true),
            route(hasAccount = true, isAddingAccount = true),
        )
    }
}
