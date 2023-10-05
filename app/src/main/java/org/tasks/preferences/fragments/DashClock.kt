package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.dialogs.FilterPicker.Companion.newFilterPicker
import org.tasks.dialogs.FilterPicker.Companion.setFilterPickerResultListener
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

@AndroidEntryPoint
class DashClock : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun getPreferenceXml() = R.xml.preferences_dashclock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFilterPickerResultListener(this) {
            defaultFilterProvider.dashclockFilter = it
            lifecycleScope.launch {
                refreshPreferences()
            }
            localBroadcastManager.broadcastRefresh()
        }
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_dashclock_filter)
            .setOnPreferenceClickListener {
                newFilterPicker(defaultFilterProvider.dashclockFilter)
                    .show(childFragmentManager, FRAG_TAG_SELECT_PICKER)
                false
            }

        refreshPreferences()
    }

    private suspend fun refreshPreferences() {
        val filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter)
        findPreference(R.string.p_dashclock_filter).summary = filter.title
    }

    companion object {
        private const val FRAG_TAG_SELECT_PICKER = "frag_tag_select_picker"
    }
}