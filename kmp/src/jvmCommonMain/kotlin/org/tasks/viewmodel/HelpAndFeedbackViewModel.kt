package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.PlatformConfiguration
import org.tasks.TasksBuildConfig
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.BillingProvider
import org.tasks.billing.PurchaseState
import org.tasks.feed.BlogFeedMode
import org.tasks.preferences.TasksPreferences

open class HelpAndFeedbackViewModel(
    private val reporting: Reporting,
    private val tasksPreferences: TasksPreferences,
    private val platformConfiguration: PlatformConfiguration,
    purchaseState: PurchaseState,
    collectStatistics: Boolean = runBlocking { tasksPreferences.get(TasksPreferences.collectStatistics, true) },
) : ViewModel() {

    val versionName: String = TasksBuildConfig.VERSION_NAME

    val isGooglePlay: Boolean =
        platformConfiguration.billingProvider == BillingProvider.GOOGLE_PLAY

    val showTermsOfService: Boolean =
        !platformConfiguration.isLibre || purchaseState.hasTasksAccount

    val showSendLogs: Boolean = platformConfiguration.supportsLogExport

    val showCollectStatistics: Boolean = !platformConfiguration.isLibre

    var collectStatistics by mutableStateOf(collectStatistics)
        protected set

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

    fun logEvent(type: String) {
        reporting.logEvent(
            AnalyticsEvents.SETTINGS_CLICK,
            AnalyticsEvents.PARAM_TYPE to type,
        )
    }

    fun updateCollectStatistics(enabled: Boolean) {
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
}
