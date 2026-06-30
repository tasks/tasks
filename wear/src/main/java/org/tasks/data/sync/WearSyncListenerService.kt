/**
 * WearSyncListenerService.kt — System-started service for Data Layer events.
 *
 * Extends [WearableListenerService], which the OS wakes whenever a
 * [DataItem] changes — even if the Wear app isn't running.
 *
 * The service simply delegates every [DataEventBuffer] to
 * [DataLayerSyncManager.onDataChanged], which routes events to the
 * appropriate handler (ack, incoming operation, snapshot, etc.).
 */
package org.tasks.data.sync

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Background service that listens for data changes from phone.
 * This service is started automatically by the system when data changes occur,
 * even if the app is not running.
 *
 * Note: The Data Layer persists data and will deliver it when the device reconnects,
 * so this doesn't need to be running all the time.
 */
class WearSyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var syncManager: DataLayerSyncManager
    private lateinit var syncRepository: SyncRepository

    override fun onCreate() {
        super.onCreate()
        syncRepository = SyncRepository.getInstance(applicationContext)
        syncManager = DataLayerSyncManager.getInstance(applicationContext)
        Timber.d("WearSyncListenerService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Timber.d("WearSyncListenerService destroyed")
    }

    /**
     * Called when data items are changed.
     * This is called even when the app is not in the foreground.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Timber.d("WearSyncListenerService: onDataChanged with ${dataEvents.count} events")

        // Delegate to the sync manager which handles the actual processing
        syncManager.onDataChanged(dataEvents)
    }
}

