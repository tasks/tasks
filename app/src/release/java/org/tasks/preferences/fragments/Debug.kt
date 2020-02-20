package org.tasks.preferences.fragments

import android.os.Bundle
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment

class Debug : InjectingPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}