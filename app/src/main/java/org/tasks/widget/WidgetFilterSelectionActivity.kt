package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.activities.FilterPicker.Companion.EXTRA_FILTER
import org.tasks.activities.FilterPicker.Companion.SELECT_FILTER
import org.tasks.activities.FilterPicker.Companion.newFilterPicker
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class WidgetFilterSelectionActivity : InjectingAppCompatActivity() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId == -1) {
            throw RuntimeException("Missing ${AppWidgetManager.EXTRA_APPWIDGET_ID}")
        }
        supportFragmentManager
            .setFragmentResultListener(SELECT_FILTER, this) { _, result ->
                if (result.isEmpty) {
                    finish()
                    return@setFragmentResultListener
                }
                result.getParcelable<Filter>(EXTRA_FILTER)?.let { filter ->
                    WidgetPreferences(this, preferences, widgetId)
                        .setFilter(defaultFilterProvider.getFilterPreferenceValue(filter))
                    localBroadcastManager.reconfigureWidget(widgetId)
                }
                setResult(RESULT_OK, Intent().putExtras(result))
                finish()
            }
        newFilterPicker(intent.getParcelableExtra(EXTRA_FILTER))
            .show(supportFragmentManager, FRAG_TAG_FILTER_PICKER)
    }

    companion object {
        private const val FRAG_TAG_FILTER_PICKER = "frag_tag_filter_picker"
    }
}