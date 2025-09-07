package org.tasks.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun reconfigureWidgets(vararg appWidgetIds: Int) = scope.launch {
        Timber.d("reconfigureWidgets(${appWidgetIds.joinToString()})")

        val ids = appWidgetIds.takeIf { it.isNotEmpty() } ?: widgetIds

        val intent = Intent(context, TasksWidget::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            .apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE }

        context.sendOrderedBroadcast(
            intent,
            null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    scope.launch {
                        Timber.d("Update widgets after reconfigure: ${appWidgetIds.joinToString { it.toString() }}")
                        // I don't like it, but this seems to give Android enough time to update
                        // the cache, and I don't have time to rewrite this in Glance right now
                        delay(100)
                        notifyAppWidgetViewDataChanged(ids)
                    }
                }
            },
            null,
            Activity.RESULT_OK,
            null,
            null
        )
    }

    fun updateWidgets() = scope.launch {
        val appWidgetIds = widgetIds
        Timber.d("updateWidgets: ${appWidgetIds.joinToString { it.toString() }}")
        notifyAppWidgetViewDataChanged(appWidgetIds)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null

    private suspend fun notifyAppWidgetViewDataChanged(appWidgetIds: IntArray) = withContext(Dispatchers.Main) {
        appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
    }
}
