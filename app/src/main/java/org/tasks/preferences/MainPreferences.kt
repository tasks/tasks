package org.tasks.preferences

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.MainSettingsFragment

class MainPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.TLA_menu_settings

    override fun getRootPreference() = MainSettingsFragment()

    override fun inject(component: ActivityComponent) {
        component.inject(this)
    }
}