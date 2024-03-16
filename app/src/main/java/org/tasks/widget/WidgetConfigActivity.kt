package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.fragments.WidgetSettings.Companion.newWidgetSettings

@AndroidEntryPoint
class WidgetConfigActivity : BasePreferences() {

    private var appWidgetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        super.onCreate(savedInstanceState)

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
    }

    override fun getRootTitle() = R.string.widget_settings

    override fun getRootPreference() = newWidgetSettings(appWidgetId)
}