package org.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.todoroo.astrid.api.AstridApiConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.throttleLatest
import org.tasks.injection.ApplicationScope
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBroadcastManager @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val appWidgetManager: AppWidgetManager,
): RefreshBroadcaster {
    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)
    private val refreshChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        refreshChannel
            .consumeAsFlow()
            .throttleLatest(1000)
            .onEach {
                localBroadcastManager.sendBroadcast(Intent(REFRESH))
                appWidgetManager.updateWidgets()
            }
            .launchIn(scope)
    }

    fun registerRefreshReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.registerReceiver(broadcastReceiver!!, IntentFilter(REFRESH))
    }

    fun registerRefreshListReceiver(broadcastReceiver: BroadcastReceiver?) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(REFRESH)
        localBroadcastManager.registerReceiver(broadcastReceiver!!, intentFilter)
    }

    fun registerTaskCompletedReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.registerReceiver(broadcastReceiver!!, IntentFilter(TASK_COMPLETED))
    }

    fun registerPurchaseReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.registerReceiver(broadcastReceiver!!, IntentFilter(REFRESH_PURCHASES))
    }

    fun registerPreferenceReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.registerReceiver(
            broadcastReceiver!!,
            IntentFilter(REFRESH_PREFERENCES)
        )
    }

    override fun broadcastRefresh() {
        refreshChannel.trySend(Unit)
    }

    fun broadcastPreferenceRefresh() {
        localBroadcastManager.sendBroadcast(Intent(REFRESH_PREFERENCES))
    }

    fun broadcastTaskCompleted(id: List<Long>, oldDueDate: Long = 0L) {
        val intent = Intent(TASK_COMPLETED)
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, ArrayList(id))
        intent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, oldDueDate)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun unregisterReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.unregisterReceiver(broadcastReceiver!!)
    }

    fun broadcastPurchasesUpdated() {
        localBroadcastManager.sendBroadcast(Intent(REFRESH_PURCHASES))
    }

    fun reconfigureWidgets() {
        appWidgetManager.widgetIds.forEach { reconfigureWidget(it) }
    }

    fun reconfigureWidget(appWidgetId: Int) {
        appWidgetManager.rebuildWidgets(appWidgetId)
    }

    companion object {
        const val REFRESH = "${BuildConfig.APPLICATION_ID}.REFRESH"
        private const val TASK_COMPLETED = "${BuildConfig.APPLICATION_ID}.REPEAT"
        private const val REFRESH_PURCHASES = "${BuildConfig.APPLICATION_ID}.REFRESH_PURCHASES"
        private const val REFRESH_PREFERENCES = "${BuildConfig.APPLICATION_ID}.REFRESH_PREFERENCES"
    }
}
