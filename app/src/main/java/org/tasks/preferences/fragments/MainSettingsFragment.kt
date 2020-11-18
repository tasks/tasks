package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.IconPreference
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment : InjectingPreferenceFragment() {

    @Inject lateinit var appWidgetManager: AppWidgetManager
    @Inject lateinit var preferences: Preferences

    private val viewModel: PreferencesViewModel by activityViewModels()

    override fun getPreferenceXml() = R.xml.preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference(R.string.synchronization).summary =
                resources
                        .getStringArray(R.array.synchronization_services)
                        .joinToString(getString(R.string.list_separator_with_space))

        viewModel.lastBackup.observe(this) { updateBackupWarning() }
        viewModel.lastAndroidBackup.observe(this) { updateBackupWarning() }
        viewModel.lastDriveBackup.observe(this) { updateBackupWarning() }
    }

    override fun onResume() {
        super.onResume()

        updateBackupWarning()
        updateWidgetVisibility()
    }

    private fun updateWidgetVisibility() {
        findPreference(R.string.widget_settings).isVisible = appWidgetManager.widgetIds.isNotEmpty()
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        requires(BuildConfig.DEBUG, R.string.debug)

        updateWidgetVisibility()
    }

    private fun updateBackupWarning() {
        val backupWarning =
                preferences.showBackupWarnings()
                        && (viewModel.usingPrivateStorage
                        || viewModel.staleLocalBackup
                        || viewModel.staleRemoteBackup)
        (findPreference(R.string.backup_BPr_header) as IconPreference).iconVisible = backupWarning
    }
}
