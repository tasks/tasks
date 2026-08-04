package org.tasks.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * WearableListenerService that receives data changes from watch.
 * This service runs in the background and receives sync operations
 * even when the Tasks app is not in the foreground.
 */
@AndroidEntryPoint
class WearSyncListenerService : WearableListenerService() {

    @Inject
    lateinit var phoneSyncManager: PhoneSyncManager

    override fun onCreate() {
        super.onCreate()
        Timber.d("WearSyncListenerService: Created")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Timber.d("WearSyncListenerService: Received ${dataEvents.count} data events")

        // Forward to the sync manager
        phoneSyncManager.onDataChanged(dataEvents)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("WearSyncListenerService: Destroyed")
    }
}

