package org.tasks.preferences

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.HelpAndFeedback

@AndroidEntryPoint
class HelpAndFeedback : BasePreferences() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val source = intent.getStringExtra(EXTRA_SOURCE) ?: "unknown"
            firebase.logEvent(R.string.event_help_and_feedback, R.string.param_source to source)
        }
    }

    override fun getRootTitle() = R.string.help_and_feedback

    override fun getRootPreference() = HelpAndFeedback()

    companion object {
        const val EXTRA_SOURCE = "extra_source"
    }
}
