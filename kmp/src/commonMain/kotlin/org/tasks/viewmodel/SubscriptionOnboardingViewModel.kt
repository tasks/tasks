package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.analytics.logCloudOnboarding
import org.tasks.compose.SubscriptionOnboardingStep
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

open class SubscriptionOnboardingViewModel(
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
    private val reporting: Reporting,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val hasTasksOrgAccount: Boolean = false,
        val tasksAccountHasList: Boolean = false,
    ) {
        val initialStep: SubscriptionOnboardingStep?
            get() = when {
                !hasTasksOrgAccount -> SubscriptionOnboardingStep.WELCOME
                !tasksAccountHasList -> SubscriptionOnboardingStep.CREATE_LIST
                else -> null
            }

        fun stepAfter(current: SubscriptionOnboardingStep): SubscriptionOnboardingStep? =
            when (current) {
                SubscriptionOnboardingStep.WELCOME -> when {
                    !tasksAccountHasList -> SubscriptionOnboardingStep.CREATE_LIST
                    else -> null
                }
                SubscriptionOnboardingStep.CREATE_LIST -> null
            }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _step = MutableStateFlow<SubscriptionOnboardingStep?>(null)
    val step: StateFlow<SubscriptionOnboardingStep?> = _step.asStateFlow()

    private var resolvedInitialStep = false

    init {
        viewModelScope.launch {
            caldavDao.watchAccounts().collect { accounts ->
                val tasksAccount = accounts.firstOrNull {
                    it.accountType == CaldavAccount.TYPE_TASKS
                }
                val tasksAccountHasList = tasksAccount?.uuid?.let { uuid ->
                    caldavDao.getCalendarsByAccount(uuid).isNotEmpty()
                } ?: false
                _state.update {
                    it.copy(
                        loading = false,
                        hasTasksOrgAccount = tasksAccount != null,
                        tasksAccountHasList = tasksAccountHasList,
                    )
                }
                advance()
            }
        }
    }

    private fun advance() {
        val state = _state.value
        if (state.loading) {
            return
        }
        if (!resolvedInitialStep) {
            resolvedInitialStep = true
            goToStep(state.initialStep)
        } else if (_step.value == SubscriptionOnboardingStep.WELCOME && state.hasTasksOrgAccount) {
            reporting.logCloudOnboarding(AnalyticsEvents.CloudOnboarding.SIGNED_IN)
            goToStep(state.stepAfter(SubscriptionOnboardingStep.WELCOME))
        }
    }

    private fun goToStep(target: SubscriptionOnboardingStep?) {
        when (target) {
            null -> {
                clearFlag()
                return
            }
            SubscriptionOnboardingStep.WELCOME ->
                reporting.logCloudOnboarding(AnalyticsEvents.CloudOnboarding.WELCOME)
            SubscriptionOnboardingStep.CREATE_LIST ->
                reporting.logCloudOnboarding(AnalyticsEvents.CloudOnboarding.CREATE_LIST)
        }
        _step.value = target
    }

    fun onSignInClicked() {
        reporting.logCloudOnboarding(AnalyticsEvents.CloudOnboarding.SIGN_IN)
    }

    fun onListCreated() {
        clearFlag()
    }

    fun dismiss() {
        clearFlag()
    }

    private fun clearFlag() {
        reporting.logCloudOnboarding(AnalyticsEvents.CloudOnboarding.DONE)
        _step.value = null
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.needsCloudOnboarding, false)
        }
    }
}
