package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

class AppWidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context)

    val widgetIds: IntArray
        get() = appWidgetManager
                ?.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
                ?: intArrayOf()

    fun reconfigureWidgets(vararg appWidgetIds: Int) = scope.launch(Dispatchers.IO) {
        val intent = Intent(context, TasksWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                appWidgetIds.takeIf { it.isNotEmpty() } ?: widgetIds)
        context.sendBroadcast(intent)
        updateWidgets()
    }

    fun updateWidgets() = scope.launch(Dispatchers.IO) {
        Timber.d("Updating widgets")
        appWidgetManager?.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null
}