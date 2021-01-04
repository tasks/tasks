package org.tasks.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.adapter.FilterAdapter
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.dialogs.DialogBuilder
import org.tasks.filters.FilterProvider
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@AndroidEntryPoint
class FilterSelectionActivity : InjectingAppCompatActivity() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var filterAdapter: FilterAdapter
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider

    private var selected: Filter? = null

    private val refreshReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val returnFilter = intent.getBooleanExtra(EXTRA_RETURN_FILTER, false)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        selected = intent.getParcelableExtra(EXTRA_FILTER)
        if (savedInstanceState != null) {
            filterAdapter.restore(savedInstanceState)
        }
        dialogBuilder
                .newDialog()
                .setSingleChoiceItems(filterAdapter, -1) { dialog, which ->
                    val selectedFilter = filterAdapter.getItem(which) as Filter
                    val data = Intent()
                    if (returnFilter) {
                        data.putExtra(EXTRA_FILTER, selectedFilter)
                    }
                    if (widgetId != -1) {
                        WidgetPreferences(this, preferences, widgetId)
                                .setFilter(defaultFilterProvider.getFilterPreferenceValue(selectedFilter))
                        localBroadcastManager.reconfigureWidget(widgetId)
                    }
                    data.putExtra(EXTRA_FILTER_NAME, selectedFilter.listingTitle)
                    data.putExtra(EXTRA_FILTER_SQL, selectedFilter.getSqlQuery())
                    if (selectedFilter.valuesForNewTasks != null) {
                        data.putExtra(
                                EXTRA_FILTER_VALUES,
                                AndroidUtilities.mapToSerializedString(selectedFilter.valuesForNewTasks))
                    }
                    setResult(Activity.RESULT_OK, data)
                    dialog.dismiss()
                }
                .setOnDismissListener { finish() }
                .show()
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        filterAdapter.save(outState)
    }

    private fun refresh() = lifecycleScope.launch {
        val items = filterProvider.filterPickerItems()
        filterAdapter.setData(items, selected)
    }

    companion object {
        const val EXTRA_RETURN_FILTER = "extra_include_filter"
        const val EXTRA_FILTER = "extra_filter"
        private const val EXTRA_FILTER_NAME = "extra_filter_name"
        private const val EXTRA_FILTER_SQL = "extra_filter_query"
        private const val EXTRA_FILTER_VALUES = "extra_filter_values"
    }
}