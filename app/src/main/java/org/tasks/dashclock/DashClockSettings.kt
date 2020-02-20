package org.tasks.dashclock

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.fragments.DashClock

class DashClockSettings : BasePreferences() {
    override fun getRootTitle() = R.string.pro_dashclock_extension

    override fun getRootPreference() = DashClock()

    override fun inject(component: ActivityComponent) = component.inject(this)
}