package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.api.Filter
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

private const val REQUEST_SELECT_FILTER = 1005
private const val REQUEST_SUBSCRIPTION = 1006

class DashClock : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var inventory: Inventory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_dashclock, rootKey)

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

        if (!inventory.purchasedDashclock()) {
            startActivityForResult(
                Intent(context, PurchaseActivity::class.java),
                REQUEST_SUBSCRIPTION
            )
        }
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
        } else if (requestCode == REQUEST_SUBSCRIPTION) {
            if (!inventory.purchasedDashclock()) {
                activity!!.finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun refreshPreferences() {
        val filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter)
        findPreference(R.string.p_dashclock_filter).summary = filter.listingTitle
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}