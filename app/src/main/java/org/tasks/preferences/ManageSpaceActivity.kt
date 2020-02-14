package org.tasks.preferences

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.Advanced
import org.tasks.preferences.fragments.HelpAndFeedback
import org.tasks.preferences.fragments.Notifications

class ManageSpaceActivity : BasePreferences() {

    override fun getRootTitle() = R.string.preferences_advanced

    override fun getRootPreference() = Advanced()

    override fun inject(component: ActivityComponent) = component.inject(this)
}