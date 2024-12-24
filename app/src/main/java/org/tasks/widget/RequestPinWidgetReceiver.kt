package org.tasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RequestPinWidgetReceiver : BroadcastReceiver() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId == -1) {
            Timber.e("Missing widgetId")
            return
        }
        val filter = intent.getStringExtra(EXTRA_FILTER)
        val color = intent.getIntExtra(EXTRA_COLOR, 0)
        val widgetPreferences = WidgetPreferences(context, preferences, widgetId)
        widgetPreferences.setFilter(filter)
        widgetPreferences.setColor(color)
        appWidgetManager.reconfigureWidgets()
    }

    companion object {
        const val ACTION_CONFIGURE_WIDGET = "org.tasks.CONFIGURE_WIDGET"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_COLOR = "extra_color"
    }
}