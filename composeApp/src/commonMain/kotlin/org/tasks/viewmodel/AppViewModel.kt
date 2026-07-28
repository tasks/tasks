package org.tasks.viewmodel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.data.dao.CaldavDao
import org.tasks.data.newLocalAccount
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import kotlin.math.roundToInt

class AppViewModel(
    private val caldavDao: CaldavDao,
    private val syncAdapters: SyncAdapters,
    private val reporting: Reporting,
    private val preferences: TasksPreferences,
) : ViewModel() {

    val hasAccount = caldavDao
        .watchAccountExists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The sidebar and pane geometry, as last stored. */
    data class Layout(
        val taskListPaneWidth: Dp = DEFAULT_TASK_LIST_PANE_WIDTH,
        val sidebarWidth: Dp = DEFAULT_SIDEBAR_WIDTH,
        val sidebarExpanded: Boolean = true,
    )

    /**
     * Null until the stored geometry has been read, so nothing renders a stale default.
     *
     * Read once and off the main thread: this view model is constructed during the first
     * composition, so a blocking read here stalls startup. Everything that changes the geometry
     * afterwards goes through the setters below, which update this and persist.
     *
     * The sidebar's geometry lives here rather than in DrawerViewModel because the first frame is
     * gated on it. Gating on a view model that also runs the drawer's filter queries meant building
     * that view model - and running those queries, for a drawer that does not exist yet - above the
     * account check, on the cold-start path of a user staring at the welcome screen.
     */
    private val _layout = MutableStateFlow<Layout?>(null)
    val layout = _layout.asStateFlow()

    fun setTaskListPaneWidth(width: Dp) {
        _layout.update { it?.copy(taskListPaneWidth = width) }
        viewModelScope.launch {
            preferences.set(TasksPreferences.taskListPaneWidth, width.value.roundToInt())
        }
    }

    fun setSidebarWidth(width: Dp) {
        _layout.update { it?.copy(sidebarWidth = width) }
        viewModelScope.launch {
            preferences.set(TasksPreferences.sidebarWidth, width.value.roundToInt())
        }
    }

    fun setSidebarExpanded(expanded: Boolean) {
        _layout.update { it?.copy(sidebarExpanded = expanded) }
        viewModelScope.launch {
            preferences.set(TasksPreferences.sidebarExpanded, expanded)
        }
    }

    private suspend fun readLayout() = Layout(
        taskListPaneWidth = preferences.get(TasksPreferences.taskListPaneWidth, 0)
            .takeIf { it > 0 }?.dp
            ?: DEFAULT_TASK_LIST_PANE_WIDTH,
        sidebarWidth = preferences.get(TasksPreferences.sidebarWidth, 0)
            .takeIf { it > 0 }?.dp
            ?: DEFAULT_SIDEBAR_WIDTH,
        sidebarExpanded = preferences.get(TasksPreferences.sidebarExpanded, true),
    )

    init {
        viewModelScope.launch {
            // Bounded and non-throwing: callers hold their first frame on this, so a store that
            // can't be read has to leave them on defaults rather than on a blank window.
            val stored = try {
                withTimeoutOrNull(PREFERENCE_READ_TIMEOUT_MS) { readLayout() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to read layout preferences" }
                null
            }
            _layout.value = stored ?: Layout()
        }
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

    companion object {
        private const val TAG = "AppViewModel"
        private val DEFAULT_TASK_LIST_PANE_WIDTH = 400.dp
        private val DEFAULT_SIDEBAR_WIDTH = 280.dp
        private const val PREFERENCE_READ_TIMEOUT_MS = 2_000L
    }
}
