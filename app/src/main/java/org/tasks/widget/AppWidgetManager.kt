package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class AppWidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val appWidgetManager: AppWidgetManager? by lazy {
        AppWidgetManager.getInstance(context)
    }

    val widgetIds: IntArray
        get() = appWidgetManager
                ?.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
                ?: intArrayOf()

    fun rebuildWidgets(vararg appWidgetIds: Int) {
        if (appWidgetIds.isEmpty()) {
            return
        }
        Timber.d("rebuildWidgets(${appWidgetIds.joinToString()})")
        val intent = Intent(context, TasksWidget::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            .apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE }
        context.sendBroadcast(intent)
    }

    fun updateWidgets() {
        rebuildWidgets(*widgetIds)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null
}
