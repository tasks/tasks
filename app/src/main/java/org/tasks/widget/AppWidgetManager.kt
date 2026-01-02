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
import org.tasks.R
import org.tasks.compose.throttleLatest
import org.tasks.injection.ApplicationScope
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
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
    private val _generation = AtomicLong(0)
    val generation: Long get() = _generation.get()

    private val updateChannel = Channel<Long>(Channel.CONFLATED)

    init {
        updateChannel
            .consumeAsFlow()
            .throttleLatest(1000)
            .onEach { gen ->
                if (gen == _generation.get()) {
                    val appWidgetIds = widgetIds
                    Timber.d("updateWidgets: ${appWidgetIds.joinToString { it.toString() }}")
                    appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
                } else {
                    Timber.d("Skipping stale widget update")
                }
            }
            .launchIn(scope)
    }

    val widgetIds: IntArray
        get() = appWidgetManager
                ?.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
                ?: intArrayOf()

    fun reconfigureWidgets(vararg appWidgetIds: Int) {
        val newGeneration = _generation.incrementAndGet()
        val ids = appWidgetIds.takeIf { it.isNotEmpty() } ?: widgetIds
        Timber.d("reconfigureWidgets(${ids.joinToString()}) generation=$newGeneration")
        val intent = Intent(context, TasksWidget::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            .apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE }
        context.sendBroadcast(intent)
    }

    fun updateWidgets() {
        updateChannel.trySend(_generation.get())
    }

    fun exists(id: Int) = appWidgetManager?.getAppWidgetInfo(id) != null
}
