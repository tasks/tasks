package org.tasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.compose.throttleLatest
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWidgetManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val appWidgetManager: AppWidgetManager? by lazy {
        AppWidgetManager.getInstance(context)
    }
    private val updateChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        updateChannel
            .consumeAsFlow()
            .throttleLatest(1000)
            .onEach { rebuildWidgets() }
            .launchIn(scope)
    }

    val widgetIds: IntArray
        get() = appWidgetManager
                ?.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
                ?: intArrayOf()

    fun rebuildWidgets(vararg appWidgetIds: Int) {
        val ids = appWidgetIds.takeIf { it.isNotEmpty() } ?: widgetIds
        Timber.d("rebuildWidgets(${ids.joinToString()})")
        val intent = Intent(context, TasksWidget::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            .apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE }
        context.sendBroadcast(intent)
    }

    fun updateWidgets() {
        updateChannel.trySend(Unit)
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null
}
