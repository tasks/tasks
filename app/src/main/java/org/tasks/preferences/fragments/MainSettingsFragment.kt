package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.auth.AuthStateManager
import org.tasks.auth.IdToken
import org.tasks.auth.SignInActivity
import org.tasks.auth.TasksAccountSettingsActivity
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
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
    @Inject lateinit var authStateManager: AuthStateManager
    @Inject lateinit var caldavDao: CaldavDao

    private val viewModel: PreferencesViewModel by activityViewModels()

    override fun getPreferenceXml() = R.xml.preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = findPreference(R.string.tasks_org) as IconPreference
        pref.setIcon(R.drawable.ic_round_icon_36dp)
        pref.iconVisible = true

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

        lifecycleScope.launch {
            updateAccount()
        }
        updateBackupWarning()
        updateWidgetVisibility()
    }

    private suspend fun updateAccount() {
        val pref = findPreference(R.string.tasks_org) as IconPreference
        pref.drawable = ContextCompat
                .getDrawable(requireContext(), R.drawable.ic_keyboard_arrow_right_24px)
                ?.mutate()
        pref.tint = context?.getColor(R.color.icon_tint_with_alpha)
        val accounts = caldavDao.getAccounts(CaldavAccount.TYPE_TASKS)
        if (accounts.isEmpty()) {
            pref.setOnPreferenceClickListener { signIn() }
            pref.summary = getString(R.string.sign_in_with_google)
            return
        }
        val idToken = authStateManager.current
                .takeIf { it.isAuthorized }
                ?.idToken
                ?.let { IdToken(it) }
        val account = idToken
                ?.let { token -> accounts.firstOrNull { it.username == "google_${token.sub}" } }
                ?: accounts.first().apply {
                    // auth state doesn't match any accounts
                    authStateManager.signOut()
                }
        pref.summary = idToken?.email ?: account.name

        if (!account.error.isNullOrBlank()) {
            pref.drawable = ContextCompat
                    .getDrawable(requireContext(), R.drawable.ic_outline_error_outline_24px)
                    ?.mutate()
            pref.tint = context?.getColor(R.color.overdue)
        }
        pref.setOnPreferenceClickListener {
            startActivity(
                    Intent(requireContext(), TasksAccountSettingsActivity::class.java)
                            .putExtra(BaseCaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account)
            )
            false
        }
    }

    private fun signIn(): Boolean {
        activity?.startActivityForResult(
                Intent(activity, SignInActivity::class.java),
                Synchronization.REQUEST_TASKS_ORG)
        return false
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
