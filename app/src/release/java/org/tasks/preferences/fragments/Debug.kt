package org.tasks.preferences.fragments

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.injection.InjectingPreferenceFragment

@AndroidEntryPoint
class Debug : InjectingPreferenceFragment() {
    override fun getPreferenceXml() = 0

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {}
}