package org.tasks.preferences.fragments

import android.os.Bundle
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment

class MainSettingsFragment : InjectingPreferenceFragment() {
    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        requires(BuildConfig.DEBUG, R.string.debug)
    }
}
