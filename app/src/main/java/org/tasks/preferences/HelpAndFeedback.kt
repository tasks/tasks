package org.tasks.preferences

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.HelpAndFeedback

@AndroidEntryPoint
class HelpAndFeedback : BasePreferences() {

    override fun getRootTitle() = R.string.help_and_feedback

    override fun getRootPreference() = HelpAndFeedback()
}