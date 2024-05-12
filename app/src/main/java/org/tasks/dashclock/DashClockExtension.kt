package org.tasks.dashclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.apps.dashclock.api.DashClockExtension
import com.google.android.apps.dashclock.api.ExtensionData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.dao.TaskDao
import org.tasks.data.count
import org.tasks.data.fetchFiltered
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DashClockExtension : DashClockExtension() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val refreshReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
        job.cancel()
    }

    override fun onUpdateData(i: Int) {
        refresh()
    }

    private fun refresh() = scope.launch {
        val filterPreference = preferences.getStringValue(R.string.p_dashclock_filter)
        val filter = defaultFilterProvider.getFilterFromPreference(filterPreference)
        val count = taskDao.count(filter)
        if (count == 0) {
            publish(null)
        } else {
            val clickIntent = TaskIntents.getTaskListIntent(this@DashClockExtension, filter)
            val extensionData = ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_check_white_24dp)
                    .status(count.toString())
                    .expandedTitle(resources.getQuantityString(R.plurals.task_count, count, count))
                    .expandedBody(filter.title)
                    .clickIntent(clickIntent)
            if (count == 1) {
                val tasks = taskDao.fetchFiltered(filter)
                if (tasks.isNotEmpty()) {
                    extensionData.expandedTitle(tasks[0].title)
                }
            }
            publish(extensionData)
        }
    }

    private fun publish(data: ExtensionData?) {
        try {
            publishUpdate(data)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}