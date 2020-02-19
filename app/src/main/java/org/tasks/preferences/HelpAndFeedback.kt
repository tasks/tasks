package org.tasks.preferences

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.HelpAndFeedback

class HelpAndFeedback : BasePreferences() {

    override fun getRootTitle() = R.string.help_and_feedback

    override fun getRootPreference() = HelpAndFeedback()

    override fun inject(component: ActivityComponent) {
        component.inject(this)
    }
}