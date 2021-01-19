package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import org.tasks.data.GoogleTaskAccount
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.IconPreference
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.preferences.fragments.TasksAccount.Companion.newTasksAccountPreference
import org.tasks.sync.AddAccountDialog.Companion.newAccountDialog
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment : InjectingPreferenceFragment() {

    @Inject lateinit var appWidgetManager: AppWidgetManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskDeleter: TaskDeleter

    private val viewModel: PreferencesViewModel by activityViewModels()

    override fun getPreferenceXml() = R.xml.preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference(R.string.add_account).setOnPreferenceClickListener { addAccount() }

        viewModel.lastBackup.observe(this) { updateBackupWarning() }
        viewModel.lastAndroidBackup.observe(this) { updateBackupWarning() }
        viewModel.lastDriveBackup.observe(this) { updateBackupWarning() }
        viewModel.googleTaskAccounts.observe(this) { refreshAccounts() }
        viewModel.caldavAccounts.observe(this) { refreshAccounts() }
    }

    override fun onResume() {
        super.onResume()

        updateBackupWarning()
        updateWidgetVisibility()
    }

    private fun refreshAccounts() {
        val caldavAccounts = viewModel.caldavAccounts.value ?: emptyList()
        val googleTaskAccounts = viewModel.googleTaskAccounts.value ?: emptyList()
        val addAccount = findPreference(R.string.add_account)
        val index = preferenceScreen.indexOf(addAccount)
        var current = 0
        caldavAccounts.forEach {
            setup(it, if (current < index) {
                preferenceScreen.getPreference(current++) as IconPreference
            } else {
                preferenceScreen.insertAt(current++)
            })
        }
        googleTaskAccounts.forEach {
            setup(it, if (current < index) {
                preferenceScreen.getPreference(current++) as IconPreference
            } else {
                preferenceScreen.insertAt(current++)
            })
        }
        preferenceScreen.removeAt(current, index - current)
        if (caldavAccounts.isEmpty() && googleTaskAccounts.isEmpty()) {
            addAccount.setTitle(R.string.not_signed_in)
            addAccount.setIcon(R.drawable.ic_outline_cloud_24px)
        } else {
            addAccount.setTitle(R.string.add_account)
            addAccount.setIcon(R.drawable.ic_outline_add_24px)
        }
        tintIcons(addAccount, requireContext().getColor(R.color.icon_tint_with_alpha))
    }

    private fun addAccount(): Boolean {
        val hasTasksAccount = viewModel.tasksAccount() != null
        newAccountDialog(this@MainSettingsFragment, REQUEST_ADD_ACCOUNT, hasTasksAccount)
                .show(parentFragmentManager, FRAG_TAG_ADD_ACCOUNT)
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

    private fun setup(account: CaldavAccount, pref: IconPreference) {
        pref.setTitle(account.prefTitle)
        pref.summary = account.name
        pref.setIcon(account.prefIcon)
        if (account.isCaldavAccount) {
            tintIcons(pref, requireContext().getColor(R.color.icon_tint_with_alpha))
        }
        pref.setOnPreferenceClickListener {
            if (account.isTasksOrg) {
                (activity as MainPreferences).startPreference(
                        this,
                        newTasksAccountPreference(account),
                        getString(R.string.tasks_org)
                )
            } else {
                val intent = Intent(context, account.accountSettingsClass).apply {
                    putExtra(BaseCaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account)
                }
                startActivityForResult(intent, REQUEST_CALDAV_SETTINGS)
            }
            false
        }
        when {
            account.isTasksOrg -> {
                pref.setOnPreferenceClickListener {
                    (activity as MainPreferences).startPreference(
                            this,
                            newTasksAccountPreference(account),
                            getString(R.string.tasks_org)
                    )
                    false
                }
            }
        }
        setupErrorIcon(pref, account.error)
    }

    private fun setup(account: GoogleTaskAccount, pref: IconPreference) {
        pref.setTitle(R.string.gtasks_GPr_header)
        pref.setIcon(R.drawable.ic_google)
        pref.summary = account.account
        setupErrorIcon(pref, account.error)
        pref.setOnPreferenceClickListener {
            dialogBuilder
                    .newDialog(account.account)
                    .setItems(
                            listOf(
                                    getString(R.string.reinitialize_account),
                                    getString(R.string.logout)
                            )
                    ) { _, which ->
                        if (which == 0) {
                            startActivityForResult(
                                    Intent(context, GtasksLoginActivity::class.java),
                                    REQUEST_GOOGLE_TASKS
                            )
                        } else {
                            logoutConfirmation(account)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            false
        }
    }

    private fun logoutConfirmation(account: GoogleTaskAccount) {
        val name = account.account
        val alertDialog = dialogBuilder
                .newDialog()
                .setMessage(R.string.logout_warning, name)
                .setPositiveButton(R.string.logout) { _, _ ->
                    lifecycleScope.launch {
                        withContext(NonCancellable) {
                            taskDeleter.delete(account)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun setupErrorIcon(pref: IconPreference, error: String?) {
        val hasError = !error.isNullOrBlank()
        pref.drawable = ContextCompat
                .getDrawable(requireContext(), if (hasError) {
                    R.drawable.ic_outline_error_outline_24px
                } else {
                    R.drawable.ic_keyboard_arrow_right_24px
                })
                ?.mutate()
        pref.tint = context?.getColor(if (hasError) {
            R.color.overdue
        } else {
            R.color.icon_tint_with_alpha
        })
    }

    companion object {
        private const val REQUEST_ADD_ACCOUNT = 10015
        private const val FRAG_TAG_ADD_ACCOUNT = "frag_tag_add_account"
        const val REQUEST_CALDAV_SETTINGS = 10013
        const val REQUEST_GOOGLE_TASKS = 10014
        const val REQUEST_TASKS_ORG = 10016

        fun PreferenceScreen.removeAt(index: Int, count: Int = 1) {
            repeat(count) {
                removePreference(getPreference(index))
            }
        }

        fun PreferenceScreen.indexOf(pref: Preference): Int =
                0.until(preferenceCount).first { pref == getPreference(it) }

        fun PreferenceScreen.insertAt(index: Int): IconPreference {
            index.until(preferenceCount).forEach {
                getPreference(it).apply { order += 1 }
            }
            return IconPreference(context).apply {
                layoutResource = R.layout.preference_icon
                order = index
                iconVisible = true
                addPreference(this)
            }
        }
    }
}
