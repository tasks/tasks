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
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.logging.FileLogger
import org.tasks.preferences.DiagnosticInfo
import org.tasks.preferences.Preferences
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HelpAndFeedbackViewModel @Inject constructor(
    private val firebase: Firebase,
    private val fileLogger: FileLogger,
    private val inventory: Inventory,
    private val diagnosticInfo: DiagnosticInfo,
    private val preferences: Preferences,
) : ViewModel() {

    val versionName: String = BuildConfig.VERSION_NAME

    val isGooglePlay: Boolean = !IS_GENERIC

    var showTermsOfService by mutableStateOf(IS_GOOGLE_PLAY || inventory.hasTasksAccount)
        private set

    var collectStatistics by mutableStateOf(preferences.isTrackingEnabled)
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

    suspend fun getLogZipFile(): File = fileLogger.getZipFile()
}
