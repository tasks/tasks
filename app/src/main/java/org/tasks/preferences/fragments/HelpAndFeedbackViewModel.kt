package org.tasks.preferences.fragments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.feed.BlogFeedMode
import org.tasks.logging.FileLogger
import org.tasks.preferences.DiagnosticInfo
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HelpAndFeedbackViewModel @Inject constructor(
    private val firebase: Firebase,
    private val fileLogger: FileLogger,
    private val inventory: Inventory,
    private val diagnosticInfo: DiagnosticInfo,
    private val preferences: Preferences,
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    val versionName: String = BuildConfig.VERSION_NAME

    val isGooglePlay: Boolean = !IS_GENERIC

    var showTermsOfService by mutableStateOf(IS_GOOGLE_PLAY || inventory.hasTasksAccount)
        private set

    var collectStatistics by mutableStateOf(preferences.isTrackingEnabled)
        private set

    var blogFeedMode by mutableStateOf(
        runBlocking {
            BlogFeedMode.fromValue(
                tasksPreferences.get(TasksPreferences.blogFeedMode, BlogFeedMode.ANNOUNCEMENTS.value)
            )
        }
    )
        private set

    var showBlogFeedModeDialog by mutableStateOf(false)
        private set

    var showRestartDialog by mutableStateOf(false)
        private set

    val debugInfo: String
        get() = diagnosticInfo.debugInfo

    fun logEvent(type: String) {
        firebase.logEvent(R.string.event_settings_click, R.string.param_type to type)
    }

    fun updateCollectStatistics(enabled: Boolean) {
        preferences.setBoolean(R.string.p_collect_statistics, enabled)
        collectStatistics = enabled
        showRestartDialog = true
    }

    fun dismissRestartDialog() {
        showRestartDialog = false
    }

    fun showBlogFeedModeDialog() {
        showBlogFeedModeDialog = true
    }

    fun dismissBlogFeedModeDialog() {
        showBlogFeedModeDialog = false
    }

    fun updateBlogFeedMode(mode: BlogFeedMode) {
        blogFeedMode = mode
        showBlogFeedModeDialog = false
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.blogFeedMode, mode.value)
            if (mode == BlogFeedMode.NONE) {
                tasksPreferences.set(TasksPreferences.blogPendingPost, "")
            }
        }
    }

    suspend fun getLogZipFile(): File = fileLogger.getZipFile()
}
