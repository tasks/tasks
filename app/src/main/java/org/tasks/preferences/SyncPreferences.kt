package org.tasks.preferences

import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.fragments.Synchronization

@AndroidEntryPoint
class SyncPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.synchronization

    override fun getRootPreference() = Synchronization()
}