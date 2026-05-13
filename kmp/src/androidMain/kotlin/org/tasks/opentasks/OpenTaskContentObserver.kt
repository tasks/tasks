package org.tasks.opentasks

import android.content.ContentResolver
import android.content.Context
import android.content.SyncStatusObserver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import co.touchlab.kermit.Logger
import org.dmfs.tasks.contract.TaskContract.Properties
import org.dmfs.tasks.contract.TaskContract.TaskLists
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.kmp.R
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class OpenTaskContentObserver(
    context: Context,
    private val syncAdapters: SyncAdapters,
    private val tasksPreferences: TasksPreferences,
) : ContentObserver(getHandler()), SyncStatusObserver {

    val authority = context.getString(R.string.opentasks_authority)

    private val isSyncOngoing: Boolean
        get() = kotlinx.coroutines.runBlocking {
            tasksPreferences.get(TasksPreferences.syncOngoing, false) ||
                    tasksPreferences.get(TasksPreferences.syncOngoingAndroid, false)
        }

    override fun onChange(selfChange: Boolean) = onChange(selfChange, null)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        when {
            selfChange || uri == null ->
                Logger.v("OpenTaskContentObserver") { "Ignoring onChange selfChange=$selfChange uri=$uri" }

            uri.getQueryParameter("caller_is_syncadapter")?.toBoolean() == true-> {
                Logger.d("OpenTaskContentObserver") { "onChange uri=$uri" }
                syncAdapters.sync(SyncSource.CONTENT_OBSERVER)
            }

            isSyncOngoing ->
                Logger.v("OpenTaskContentObserver") { "Ignoring onChange uri=$uri sync in progress" }

            else -> {
                Logger.d("OpenTaskContentObserver") { "onChange uri=$uri" }
                syncAdapters.sync(SyncSource.CONTENT_OBSERVER)
            }
        }
    }

    override fun onStatusChanged(which: Int) {
        syncAdapters.setOpenTaskSyncActive(
                ContentResolver.getCurrentSyncs().any { it.authority == authority }
        )
    }

    companion object {
        fun getHandler() = HandlerThread("OT-handler").let {
            it.start()
            Handler(it.looper)
        }

        fun registerObserver(context: Context, observer: OpenTaskContentObserver) {
            getUris(observer.authority).forEach {
                context.contentResolver.registerContentObserver(it, false, observer)
            }
            ContentResolver.addStatusChangeListener(
                    ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE,
                    observer
            )
        }

        private fun getUris(authority: String): List<Uri> =
                listOf(TaskLists.getContentUri(authority),
                        Tasks.getContentUri(authority),
                        Properties.getContentUri(authority))
    }
}
