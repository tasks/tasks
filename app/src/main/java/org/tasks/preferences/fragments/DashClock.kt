package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.api.Filter
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

private const val REQUEST_SELECT_FILTER = 1005

class DashClock : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun getPreferenceXml() = R.xml.preferences_dashclock

    override fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_dashclock_filter)
            .setOnPreferenceClickListener {
                val intent = Intent(context, FilterSelectionActivity::class.java)
                intent.putExtra(
                    FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.dashclockFilter
                )
                intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true)
                startActivityForResult(intent, REQUEST_SELECT_FILTER)
                false
            }

        refreshPreferences()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SELECT_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                val filter: Filter =
                    data!!.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER)!!
                defaultFilterProvider.dashclockFilter = filter
                refreshPreferences()
                localBroadcastManager.broadcastRefresh()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun refreshPreferences() {
        val filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter)
        findPreference(R.string.p_dashclock_filter).summary = filter?.listingTitle
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}