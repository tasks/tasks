package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.data.dao.CaldavDao
import org.tasks.data.newLocalAccount
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class AppViewModel(
    private val caldavDao: CaldavDao,
    private val syncAdapters: SyncAdapters,
    private val reporting: Reporting,
    private val preferences: TasksPreferences,
) : ViewModel() {

    val hasAccount = caldavDao
        .watchAccountExists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            syncAdapters.sync(SyncSource.APP_RESUME)
        }
        viewModelScope.launch {
            caldavDao.watchAccounts()
                .map { accounts -> accounts.firstOrNull { it.isTasksOrg }?.username }
                .distinctUntilChanged()
                .collect { username ->
                    if (username != null) {
                        reporting.identify(username)
                    }
                }
        }
        viewModelScope.launch {
            var wasInOnboarding = false
            hasAccount.collect { state ->
                when (state) {
                    false -> wasInOnboarding = true
                    true -> {
                        if (wasInOnboarding) {
                            wasInOnboarding = false
                            val alreadyLogged = preferences.get(
                                TasksPreferences.hasLoggedOnboardingComplete,
                                false,
                            )
                            if (!alreadyLogged) {
                                reporting.logEvent(AnalyticsEvents.ONBOARDING_COMPLETE)
                                preferences.set(
                                    TasksPreferences.hasLoggedOnboardingComplete,
                                    true,
                                )
                            }
                        }
                    }
                    null -> {}
                }
            }
        }
    }

    fun continueWithoutSync() {
        viewModelScope.launch {
            caldavDao.newLocalAccount()
        }
    }
}
