package org.tasks.preferences.fragments

import android.os.Bundle
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment

class MainSettingsFragment : InjectingPreferenceFragment() {
    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun getPreferenceXml() = R.xml.preferences

    override fun setupPreferences(savedInstanceState: Bundle?) {
        requires(BuildConfig.DEBUG, R.string.debug)
    }
}
