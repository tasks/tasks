package org.tasks.preferences.fragments

import android.os.Bundle
import org.tasks.R
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment

class NavigationDrawer : InjectingPreferenceFragment() {

    override fun getPreferenceXml() = R.xml.preferences_navigation_drawer

    override fun setupPreferences(savedInstanceState: Bundle?) {

    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}