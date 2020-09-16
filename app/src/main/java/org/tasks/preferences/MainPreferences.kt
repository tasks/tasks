package org.tasks.preferences

import android.app.Activity
import android.content.Intent
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.jobs.WorkManager
import org.tasks.preferences.fragments.MainSettingsFragment
import org.tasks.preferences.fragments.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.REQUEST_GOOGLE_TASKS
import org.tasks.sync.SyncAdapters
import org.tasks.ui.Toaster
import javax.inject.Inject

@AndroidEntryPoint
class MainPreferences : BasePreferences() {

    @Inject lateinit var syncAdapters: SyncAdapters
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var toaster: Toaster

    override fun getRootTitle() = R.string.TLA_menu_settings

    override fun getRootPreference() = MainSettingsFragment()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CALDAV_SETTINGS) {
            if (resultCode == RESULT_OK) {
                syncAdapters.sync(true)
                workManager.updateBackgroundSync()
            }
        } else if (requestCode == REQUEST_GOOGLE_TASKS) {
            if (resultCode == Activity.RESULT_OK) {
                syncAdapters.sync(true)
                workManager.updateBackgroundSync()
            } else {
                data?.getStringExtra(GtasksLoginActivity.EXTRA_ERROR)?.let { toaster.longToast(it) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}