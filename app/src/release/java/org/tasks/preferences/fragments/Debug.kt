package org.tasks.preferences.fragments

import android.os.Bundle
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment

class Debug : InjectingPreferenceFragment() {

    override fun getPreferenceXml() = 0

    override fun setupPreferences(savedInstanceState: Bundle?) {}

    override fun inject(component: FragmentComponent) {}
}