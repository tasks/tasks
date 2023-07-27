package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import javax.inject.Inject

class AppWidgetManager @Inject constructor(
        @param:ApplicationContext private val context: Context
) {
    private val appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context)

    val widgetIds: IntArray
        get() = appWidgetManager
                ?.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
                ?: intArrayOf()

    fun reconfigureWidgets(vararg appWidgetIds: Int) {
        val intent = Intent(context, TasksWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                appWidgetIds.takeIf { it.isNotEmpty() } ?: widgetIds)
        context.sendBroadcast(intent)
        updateWidgets()
    }

    fun updateWidgets() {
        appWidgetManager?.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null
}