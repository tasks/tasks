package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.compose.throttleLatest
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

class AppWidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context)
    private val updateChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        updateChannel
            .consumeAsFlow()
            .throttleLatest(1000)
            .onEach {
                val appWidgetIds = widgetIds
                Timber.d("updateWidgets: ${appWidgetIds.joinToString { it.toString() }}")
                notifyAppWidgetViewDataChanged(appWidgetIds)
            }
            .launchIn(scope)
    }

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

        context.sendBroadcast(intent)
    }

    fun updateWidgets() {
        updateChannel.trySend(Unit)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null

    private suspend fun notifyAppWidgetViewDataChanged(appWidgetIds: IntArray) = withContext(Dispatchers.Main) {
        appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
    }
}
