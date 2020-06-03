package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import com.todoroo.astrid.service.TaskDeleter
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskListDao
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.sync.AddAccountDialog
import org.tasks.ui.Toaster
import javax.inject.Inject

const val REQUEST_CALDAV_SETTINGS = 10013
const val REQUEST_GOOGLE_TASKS = 10014

class Synchronization : InjectingPreferenceFragment() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var taskDeleter: TaskDeleter

    override fun getPreferenceXml() = R.xml.preferences_synchronization

    override fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_background_sync_unmetered_only)
            .setOnPreferenceChangeListener { _: Preference?, o: Any? ->
                workManager.updateBackgroundSync(null, null, o as Boolean?)
                true
            }
        findPreference(R.string.p_background_sync)
            .setOnPreferenceChangeListener { _: Preference?, o: Any? ->
                workManager.updateBackgroundSync(null, o as Boolean?, null)
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
                AddAccountDialog.showAddAccountDialog(activity, dialogBuilder)
                false
            }
    }

    override fun onResume() {
        super.onResume()

        refresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CALDAV_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                workManager.sync(true)
                workManager.updateBackgroundSync()
            }
        } else if (requestCode == REQUEST_GOOGLE_TASKS) {
            if (resultCode == Activity.RESULT_OK) {
                workManager.sync(true)
                workManager.updateBackgroundSync()
            } else if (data != null) {
                toaster.longToast(data.getStringExtra(GtasksLoginActivity.EXTRA_ERROR))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addGoogleTasksAccounts(category: PreferenceCategory): Boolean {
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
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                false
            }
            category.addPreference(preference)
        }
        return accounts.isNotEmpty()
    }

    private fun addCaldavAccounts(category: PreferenceCategory): Boolean {
        val accounts: List<CaldavAccount> = caldavDao.getAccounts().filter {
            it.accountType != TYPE_LOCAL
        }
        for (account in accounts) {
            val preference = Preference(context)
            preference.title = account.name
            val error = account.error
            if (isNullOrEmpty(error)) {
                preference.setSummary(
                    if (account.isCaldavAccount) R.string.caldav else R.string.etesync
                )
            } else {
                preference.summary = error
            }
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(
                    context,
                    if (account.isCaldavAccount) CaldavAccountSettingsActivity::class.java
                    else EteSyncAccountSettingsActivity::class.java
                )
                intent.putExtra(CaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account)
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
                taskDeleter.delete(account)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun refresh() {
        val synchronizationPreferences = findPreference(R.string.accounts) as PreferenceCategory
        synchronizationPreferences.removeAll()

        val hasGoogleAccounts: Boolean = addGoogleTasksAccounts(synchronizationPreferences)
        val hasCaldavAccounts = addCaldavAccounts(synchronizationPreferences)
        findPreference(R.string.gtasks_GPr_header).isVisible = hasGoogleAccounts
        val syncEnabled = hasGoogleAccounts || hasCaldavAccounts
        findPreference(R.string.accounts).isVisible = syncEnabled
        findPreference(R.string.sync_SPr_interval_title).isVisible = syncEnabled
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}