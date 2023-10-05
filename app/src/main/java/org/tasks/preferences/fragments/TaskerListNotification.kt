package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.FilterPicker.Companion.newFilterPicker
import org.tasks.dialogs.FilterPicker.Companion.setFilterPickerResultListener
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.bundle.ListNotificationBundle
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

@AndroidEntryPoint
class TaskerListNotification : InjectingPreferenceFragment() {

    companion object {
        const val EXTRA_FILTER = "extra_filter"
        private const val REQUEST_SUBSCRIPTION = 10125
        private const val FRAG_TAG_FILTER_PICKER = "frag_tag_filter_picker"

        fun newTaskerListNotification(filter: String?): TaskerListNotification {
            val fragment = TaskerListNotification()
            val args = Bundle()
            args.putString(EXTRA_FILTER, filter)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var inventory: Inventory

    lateinit var filter: Filter
    var cancelled: Boolean = false

    override fun getPreferenceXml() = R.xml.preferences_tasker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFilterPickerResultListener(this) {
            filter = it
            refreshPreferences()
        }
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        filter = if (savedInstanceState == null) {
            defaultFilterProvider.getFilterFromPreferenceBlocking(arguments?.getString(EXTRA_FILTER))
        } else {
            savedInstanceState.getParcelable(EXTRA_FILTER)!!
        }

        refreshPreferences()

        findPreference(R.string.filter).setOnPreferenceClickListener {
            newFilterPicker(filter)
                .show(childFragmentManager, FRAG_TAG_FILTER_PICKER)
            false
        }

        if (!inventory.purchasedTasker()) {
            startActivityForResult(
                Intent(context, PurchaseActivity::class.java),
                REQUEST_SUBSCRIPTION
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SUBSCRIPTION -> if (!inventory.purchasedTasker()) {
                cancelled = true
                requireActivity().finish()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(EXTRA_FILTER, filter)
    }

    private fun refreshPreferences() {
        findPreference(R.string.filter).summary = filter.title
    }

    fun getResultBlurb(): String? = filter.title

    fun getBundle(): Bundle =
            ListNotificationBundle.generateBundle(defaultFilterProvider.getFilterPreferenceValue(filter))
}