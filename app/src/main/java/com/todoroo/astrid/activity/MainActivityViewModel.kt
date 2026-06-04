package com.todoroo.astrid.activity

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.activity.MainActivity.Companion.LOAD_FILTER
import com.todoroo.astrid.activity.MainActivity.Companion.OPEN_FILTER
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.compose.HomeDestination
import org.tasks.compose.SubscriptionOnboardingDestination
import org.tasks.compose.WelcomeDestination
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Task
import org.tasks.data.getOrCreateLocalAccount
import org.tasks.filters.Filter
import org.tasks.filters.SearchFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.DrawerViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val defaultFilterProvider: DefaultFilterProvider,
    @ApplicationContext private val applicationContext: Context,
    private val caldavDao: CaldavDao,
    private val accountDataRepository: TasksAccountDataRepository,
    private val firebase: Firebase,
    private val tasksPreferences: TasksPreferences,
    val drawerViewModel: DrawerViewModel,
) : ViewModel() {

    data class State(
        val filter: Filter,
        val task: Task? = null,
    )

    private val _state = MutableStateFlow(
        State(
            filter = savedStateHandle.get<Filter>(OPEN_FILTER)
                ?: savedStateHandle.get<String>(LOAD_FILTER)?.let {
                    runBlocking { defaultFilterProvider.getFilterFromPreference(it) }
                }
                ?: runBlocking { defaultFilterProvider.getStartupFilter() },
            task = savedStateHandle.get<Task>(EXTRA_TASK),
        )
    )
    val state = _state.asStateFlow()

    companion object {
        private const val EXTRA_TASK = "extra_task"
        private const val KEY_WAS_IN_ONBOARDING = "was_in_onboarding"
        private const val KEY_WAS_IN_CLOUD_ONBOARDING = "was_in_cloud_onboarding"

        fun routeOnboarding(
            state: OnboardingState,
            hasAccount: Boolean?,
            needsCloudOnboarding: Boolean?,
            isImporting: Boolean,
        ): OnboardingRouting {
            if (needsCloudOnboarding == null) {
                return OnboardingRouting(state)
            }
            if (needsCloudOnboarding) {
                return if (!state.wasInCloudOnboarding) {
                    OnboardingRouting(
                        state = state.copy(wasInCloudOnboarding = true),
                        navigation = OnboardingNavigation.Push(SubscriptionOnboardingDestination),
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
                val destination = if (hasAccount) HomeDestination else WelcomeDestination
                return OnboardingRouting(
                    state = state.copy(
                        wasInCloudOnboarding = false,
                        wasInOnboarding = !hasAccount,
                    ),
                    navigation = OnboardingNavigation.ClearBackStack(destination),
                    logOnboardingComplete = hasAccount,
                    ready = true,
                )
            }
            return when (hasAccount) {
                false ->
                    if (!state.wasInOnboarding) {
                        OnboardingRouting(
                            state = state.copy(wasInOnboarding = true),
                            navigation = OnboardingNavigation.ClearBackStack(WelcomeDestination),
                            ready = true,
                        )
                    } else {
                        OnboardingRouting(state, ready = true)
                    }
                true -> when {
                    isImporting -> OnboardingRouting(state)
                    state.wasInOnboarding -> OnboardingRouting(
                        state = state.copy(wasInOnboarding = false),
                        navigation = OnboardingNavigation.ClearBackStack(HomeDestination),
                        logOnboardingComplete = true,
                        ready = true,
                    )
                    else -> OnboardingRouting(state, ready = true)
                }
                null -> OnboardingRouting(state, ready = false)
            }
        }
    }

    val accountExists: Flow<Boolean>
        get() = caldavDao.watchAccountExists()

    val needsCloudOnboarding: StateFlow<Boolean?> =
        tasksPreferences.flow(TasksPreferences.needsCloudOnboarding, false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var wasInOnboarding: Boolean
        get() = savedStateHandle[KEY_WAS_IN_ONBOARDING] ?: false
        set(value) { savedStateHandle[KEY_WAS_IN_ONBOARDING] = value }

    private var wasInCloudOnboarding: Boolean
        get() = savedStateHandle[KEY_WAS_IN_CLOUD_ONBOARDING] ?: false
        set(value) { savedStateHandle[KEY_WAS_IN_CLOUD_ONBOARDING] = value }

    data class OnboardingState(
        val wasInOnboarding: Boolean = false,
        val wasInCloudOnboarding: Boolean = false,
    )

    sealed interface OnboardingNavigation {
        data class Push(val destination: Any) : OnboardingNavigation
        data class ClearBackStack(val destination: Any) : OnboardingNavigation
    }

    data class OnboardingRouting(
        val state: OnboardingState,
        val navigation: OnboardingNavigation? = null,
        val logOnboardingComplete: Boolean = false,
        val ready: Boolean? = null,
    )

    fun routeOnboarding(
        hasAccount: Boolean?,
        needsCloudOnboarding: Boolean?,
        isImporting: Boolean,
    ): OnboardingRouting {
        val routing = routeOnboarding(
            OnboardingState(wasInOnboarding, wasInCloudOnboarding),
            hasAccount,
            needsCloudOnboarding,
            isImporting,
        )
        wasInOnboarding = routing.state.wasInOnboarding
        wasInCloudOnboarding = routing.state.wasInCloudOnboarding
        return routing
    }

    suspend fun logOnboardingComplete() {
        if (!tasksPreferences.get(TasksPreferences.hasLoggedOnboardingComplete, false)) {
            firebase.logEvent(R.string.event_onboarding_complete)
            tasksPreferences.set(TasksPreferences.hasLoggedOnboardingComplete, true)
        }
    }

    init {
        drawerViewModel.setSelectedFilter(_state.value.filter)
    }

    suspend fun resetFilter() {
        setFilter(defaultFilterProvider.getDefaultOpenFilter())
    }

    fun setFilter(
        filter: Filter,
        task: Task? = null,
    ) {
        if (filter == _state.value.filter && task == null) {
            return
        }
        savedStateHandle[EXTRA_TASK] = task
        _state.update {
            it.copy(
                filter = filter,
                task = task,
            )
        }
        drawerViewModel.setSelectedFilter(filter)
        if (filter !is SearchFilter) {
            defaultFilterProvider.setLastViewedFilter(filter)
        }
    }

    fun setTask(task: Task?) {
        savedStateHandle[EXTRA_TASK] = task
        _state.update { it.copy(task = task) }
    }

    suspend fun getAccount(id: Long) = caldavDao.getAccount(id)

    suspend fun getOrCreateLocalAccount(): CaldavAccount = caldavDao.getOrCreateLocalAccount()

    suspend fun isTasksGuest(): Boolean =
        try {
            val response = accountDataRepository.getAccountResponse()
                ?: accountDataRepository.fetchAndCache()
            response?.guest ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check guest status")
            false
        }

    fun openLastViewedFilter() = viewModelScope.launch {
        setFilter(defaultFilterProvider.getLastViewedFilter())
    }
}
