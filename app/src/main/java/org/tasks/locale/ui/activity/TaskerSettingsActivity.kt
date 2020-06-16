package org.tasks.locale.ui.activity

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.locale.bundle.ListNotificationBundle
import org.tasks.preferences.fragments.TaskerListNotification
import org.tasks.preferences.fragments.TaskerListNotification.Companion.newTaskerListNotification

@AndroidEntryPoint
class TaskerSettingsActivity : AbstractFragmentPluginPreference() {

    var filter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isLocalePluginIntent(intent)) {
            val previousBundle = getPreviousBundle()
            if (isBundleValid(previousBundle)) {
                filter = ListNotificationBundle.getFilter(previousBundle)
            }
        }

        super.onCreate(savedInstanceState)
    }

    override fun isBundleValid(bundle: Bundle?) = ListNotificationBundle.isBundleValid(bundle)

    override fun getResultBundle() = getFragment().getBundle()

    override fun getResultBlurb(bundle: Bundle?) = getFragment().getResultBlurb()

    override fun isCancelled() = getFragment().cancelled

    override fun getRootTitle() = R.string.tasker_list_notification

    override fun getRootPreference() = newTaskerListNotification(filter)

    private fun getFragment() =
        supportFragmentManager.findFragmentById(R.id.settings) as TaskerListNotification
}