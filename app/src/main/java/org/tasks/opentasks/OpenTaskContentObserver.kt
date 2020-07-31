package org.tasks.opentasks

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import org.dmfs.tasks.contract.TaskContract.*
import org.tasks.R
import org.tasks.sync.SyncAdapters
import timber.log.Timber
import javax.inject.Inject

class OpenTaskContentObserver @Inject constructor(
        private val syncAdapters: SyncAdapters
) : ContentObserver(getHandler()) {

    override fun onChange(selfChange: Boolean) = onChange(selfChange, null)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (selfChange || uri == null) {
            Timber.d("Ignoring onChange(selfChange = $selfChange, uri = $uri)")
            return
        } else {
            Timber.v("onChange($selfChange, $uri)")
        }

        syncAdapters.syncOpenTasks()
    }

    companion object {
        fun getHandler() = HandlerThread("OT-handler)").let {
            it.start()
            Handler(it.looper)
        }

        fun registerObserver(context: Context, observer: ContentObserver) {
            getUris(context.getString(R.string.opentasks_authority))
                    .forEach {
                        context.contentResolver.registerContentObserver(it, false, observer)
                    }
        }

        private fun getUris(authority: String): List<Uri> =
                listOf(TaskLists.getContentUri(authority),
                        Tasks.getContentUri(authority),
                        Properties.getContentUri(authority))
    }
}
