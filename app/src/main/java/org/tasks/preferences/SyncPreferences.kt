package org.tasks.preferences

import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.Synchronization

class SyncPreferences : BasePreferences() {

    override fun getRootTitle() = R.string.synchronization

    override fun getRootPreference() = Synchronization()

    override fun inject(component: ActivityComponent) = component.inject(this)
}