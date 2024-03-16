package org.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.common.collect.Lists
import com.todoroo.astrid.api.AstridApiConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

class LocalBroadcastManager @Inject constructor(
    @ApplicationContext context: Context,
    private val appWidgetManager: AppWidgetManager,
) {
    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)

    fun registerRefreshReceiver(broadcastReceiver: BroadcastReceiver?) {
        localBroadcastManager.registerReceiver(broadcastReceiver!!, IntentFilter(REFRESH))
    }

    fun registerRefreshListReceiver(broadcastReceiver: BroadcastReceiver?) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(REFRESH)
        intentFilter.addAction(REFRESH_LIST)
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

    fun broadcastRefresh() {
        localBroadcastManager.sendBroadcast(Intent(REFRESH))
        appWidgetManager.updateWidgets()
    }

    fun broadcastRefreshList() {
        localBroadcastManager.sendBroadcast(Intent(REFRESH_LIST))
    }

    fun broadcastPreferenceRefresh() {
        localBroadcastManager.sendBroadcast(Intent(REFRESH_PREFERENCES))
    }

    fun broadcastTaskCompleted(id: Long, oldDueDate: Long) {
        broadcastTaskCompleted(Lists.newArrayList(id), oldDueDate)
    }

    fun broadcastTaskCompleted(id: ArrayList<Long>) {
        broadcastTaskCompleted(id, 0)
    }

    private fun broadcastTaskCompleted(id: ArrayList<Long>, oldDueDate: Long) {
        val intent = Intent(TASK_COMPLETED)
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, id)
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
        appWidgetManager.reconfigureWidgets(appWidgetId)
    }

    companion object {
        const val REFRESH = "${BuildConfig.APPLICATION_ID}.REFRESH"
        const val REFRESH_LIST = "${BuildConfig.APPLICATION_ID}.REFRESH_LIST"
        private const val TASK_COMPLETED = "${BuildConfig.APPLICATION_ID}.REPEAT"
        private const val REFRESH_PURCHASES = "${BuildConfig.APPLICATION_ID}.REFRESH_PURCHASES"
        private const val REFRESH_PREFERENCES = "${BuildConfig.APPLICATION_ID}.REFRESH_PREFERENCES"
    }
}
