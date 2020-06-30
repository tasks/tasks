package org.tasks.preferences.fragments

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment

@AndroidEntryPoint
class NavigationDrawer : InjectingPreferenceFragment() {

    override fun getPreferenceXml() = R.xml.preferences_navigation_drawer

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {}
}