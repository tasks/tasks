package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.filters.Filter
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.bundle.ListNotificationBundle
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

@AndroidEntryPoint
class TaskerListNotification : InjectingPreferenceFragment() {

    companion object {
        const val EXTRA_FILTER = "extra_filter"
        private const val REQUEST_SUBSCRIPTION = 10125

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
    private val listPickerLauncher = registerForListPickerResult {
        filter = it
        refreshPreferences()
    }

    override fun getPreferenceXml() = R.xml.preferences_tasker

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        filter = if (savedInstanceState == null) {
            defaultFilterProvider.getFilterFromPreferenceBlocking(arguments?.getString(EXTRA_FILTER))
        } else {
            savedInstanceState.getParcelable(EXTRA_FILTER)!!
        }

        refreshPreferences()

        findPreference(R.string.filter).setOnPreferenceClickListener {
            listPickerLauncher.launch(
                context = requireContext(),
                selectedFilter = filter,
            )
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