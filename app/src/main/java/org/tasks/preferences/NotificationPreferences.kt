package org.tasks.preferences

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.Notifications

@AndroidEntryPoint
class NotificationPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.notifications

    override fun getRootPreference() = Notifications()
}