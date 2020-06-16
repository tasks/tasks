package org.tasks.dashclock

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.fragments.DashClock

@AndroidEntryPoint
class DashClockSettings : BasePreferences() {
    override fun getRootTitle() = R.string.pro_dashclock_extension

    override fun getRootPreference() = DashClock()
}