package org.tasks.preferences

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.MainSettingsFragment

@AndroidEntryPoint
class MainPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.TLA_menu_settings

    override fun getRootPreference() = MainSettingsFragment()
}