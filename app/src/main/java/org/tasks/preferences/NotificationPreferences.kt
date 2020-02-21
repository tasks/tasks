package org.tasks.preferences

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.Notifications

class NotificationPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.notifications

    override fun getRootPreference() = Notifications()

    override fun inject(component: ActivityComponent) = component.inject(this)
}