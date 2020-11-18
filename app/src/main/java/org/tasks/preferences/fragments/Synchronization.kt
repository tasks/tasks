package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.OpenTaskDao.Companion.ACCOUNT_TYPE_DAVx5
import org.tasks.data.OpenTaskDao.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.jobs.WorkManager
import org.tasks.opentasks.OpenTaskAccountSettingsActivity
import org.tasks.preferences.Preferences
import org.tasks.sync.AddAccountDialog.Companion.newAccountDialog
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

@AndroidEntryPoint
class Synchronization : InjectingPreferenceFragment() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var syncAdapters: SyncAdapters

    override fun getPreferenceXml() = R.xml.preferences_synchronization

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_background_sync_unmetered_only)
            .setOnPreferenceChangeListener { _: Preference?, o: Any? ->
                lifecycleScope.launch {
                    workManager.updateBackgroundSync(null, o as Boolean?)
                }
                true
            }
        findPreference(R.string.p_background_sync)
            .setOnPreferenceChangeListener { _: Preference?, o: Any? ->
                lifecycleScope.launch {
                    workManager.updateBackgroundSync(o as Boolean?, null)
                }
                true
            }

        val positionHack =
            findPreference(R.string.google_tasks_position_hack) as SwitchPreferenceCompat
        positionHack.isChecked = preferences.isPositionHackEnabled
        positionHack.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                if (newValue == null) {
                    false
                } else {
                    preferences.setLong(
                        R.string.p_google_tasks_position_hack,
                        if (newValue as Boolean) DateUtilities.now() else 0
                    )
                    true
                }
            }

        findPreference(R.string.add_account)
            .setOnPreferenceClickListener {
                newAccountDialog(this@Synchronization, REQUEST_ADD_ACCOUNT)
                        .show(parentFragmentManager, FRAG_TAG_ADD_ACCOUNT)
                false
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ADD_ACCOUNT) {
            refresh()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()

        refresh()
    }

    private suspend fun addGoogleTasksAccounts(category: PreferenceCategory): Boolean {
        val accounts: List<GoogleTaskAccount> = googleTaskListDao.getAccounts()
        for (googleTaskAccount in accounts) {
            val account = googleTaskAccount.account
            val preference = Preference(context)
            preference.title = account
            val error = googleTaskAccount.error
            if (isNullOrEmpty(error)) {
                preference.setSummary(R.string.gtasks_GPr_header)
            } else {
                preference.summary = error
            }
            preference.setOnPreferenceClickListener {
                dialogBuilder
                    .newDialog(account)
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
                            logoutConfirmation(googleTaskAccount)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                false
            }
            category.addPreference(preference)
        }
        return accounts.isNotEmpty()
    }

    private suspend fun addCaldavAccounts(category: PreferenceCategory): Boolean {
        val accounts = caldavDao.getAccounts().filter {
            it.accountType != TYPE_LOCAL && it.accountType != TYPE_TASKS
        }
        for (account in accounts) {
            val preference = Preference(context)
            preference.title = account.name
            val error = account.error
            if (isNullOrEmpty(error)) {
                preference.setSummary(when {
                    account.isCaldavAccount -> R.string.caldav
                    account.isEteSyncAccount
                            || (account.isOpenTasks
                            && account.uuid?.startsWith(ACCOUNT_TYPE_ETESYNC) == true) ->
                        R.string.etesync
                    account.isOpenTasks
                            && account.uuid?.startsWith(ACCOUNT_TYPE_DAVx5) == true ->
                        R.string.davx5
                    else -> 0
                })
            } else {
                preference.summary = error
            }
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(context, when {
                    account.isCaldavAccount -> CaldavAccountSettingsActivity::class.java
                    account.isEteSyncAccount -> EteSyncAccountSettingsActivity::class.java
                    account.isOpenTasks -> OpenTaskAccountSettingsActivity::class.java
                    else -> throw IllegalArgumentException("Unexpected account type: $account")
                })
                intent.putExtra(BaseCaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account)
                startActivityForResult(intent, REQUEST_CALDAV_SETTINGS)
                false
            }
            category.addPreference(preference)
        }
        return accounts.isNotEmpty()
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
                    refresh()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val synchronizationPreferences = findPreference(R.string.accounts) as PreferenceCategory
            synchronizationPreferences.removeAll()
            val hasGoogleAccounts: Boolean = addGoogleTasksAccounts(synchronizationPreferences)
            val hasCaldavAccounts = addCaldavAccounts(synchronizationPreferences)
            findPreference(R.string.gtasks_GPr_header).isVisible = hasGoogleAccounts
            val syncEnabled = hasGoogleAccounts || hasCaldavAccounts
            findPreference(R.string.accounts).isVisible = syncEnabled
            findPreference(R.string.sync_SPr_interval_title).isVisible = syncEnabled
        }
    }

    companion object {
        const val REQUEST_CALDAV_SETTINGS = 10013
        const val REQUEST_GOOGLE_TASKS = 10014
        private const val REQUEST_ADD_ACCOUNT = 10015
        const val REQUEST_TASKS_ORG = 10016
        private const val FRAG_TAG_ADD_ACCOUNT = "frag_tag_add_account"
    }
}