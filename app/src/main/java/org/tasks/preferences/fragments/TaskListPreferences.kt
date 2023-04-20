package org.tasks.preferences.fragments

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class TaskListPreferences : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences

    override fun getPreferenceXml() = R.xml.preferences_task_list

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {}
}