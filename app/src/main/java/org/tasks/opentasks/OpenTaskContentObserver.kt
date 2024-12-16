package org.tasks.opentasks

import android.content.ContentResolver
import android.content.Context
import android.content.SyncStatusObserver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dmfs.tasks.contract.TaskContract.Properties
import org.dmfs.tasks.contract.TaskContract.TaskLists
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import timber.log.Timber
import javax.inject.Inject

class OpenTaskContentObserver @Inject constructor(
    @ApplicationContext context: Context,
    private val syncAdapters: SyncAdapters,
    private val preferences: Preferences,
) : ContentObserver(getHandler()), SyncStatusObserver {

    val authority = context.getString(R.string.opentasks_authority)

    override fun onChange(selfChange: Boolean) = onChange(selfChange, null)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        when {
            selfChange || uri == null ->
                Timber.v("Ignoring onChange selfChange=$selfChange uri=$uri")

            uri.getQueryParameter("caller_is_syncadapter")?.toBoolean() == true-> {
                Timber.d("onChange uri=$uri")
                syncAdapters.sync(immediate = true)
            }

            preferences.isSyncOngoing ->
                Timber.v("Ignoring onChange uri=$uri sync in progress")

            else -> {
                Timber.d("onChange uri=$uri")
                syncAdapters.sync(immediate = true)
            }
        }
    }

    override fun onStatusChanged(which: Int) {
        syncAdapters.setOpenTaskSyncActive(
                ContentResolver.getCurrentSyncs().any { it.authority == authority }
        )
    }

    companion object {
        fun getHandler() = HandlerThread("OT-handler)").let {
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
