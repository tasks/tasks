package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class TaskListPreferences : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences

    override fun getPreferenceXml() = R.xml.preferences_task_list

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        val sortGroups = findPreference(R.string.p_disable_sort_groups) as SwitchPreferenceCompat
        sortGroups.isChecked = sortGroups.isChecked || preferences.usePagedQueries()
        findPreference(R.string.p_use_paged_queries).setOnPreferenceChangeListener { _, value ->
            sortGroups.isChecked = value as Boolean
            true
        }
    }
}