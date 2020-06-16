package org.tasks.preferences

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.Advanced

@AndroidEntryPoint
class ManageSpaceActivity : BasePreferences() {

    override fun getRootTitle() = R.string.preferences_advanced

    override fun getRootPreference() = Advanced()
}