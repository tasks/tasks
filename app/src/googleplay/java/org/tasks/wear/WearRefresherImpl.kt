package org.tasks.wear

import androidx.datastore.core.DataStore
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.GrpcProto.LastUpdate
import org.tasks.copy
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class WearRefresherImpl(
    phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
    private val registry: WearDataLayerRegistry,
    private val scope: CoroutineScope,
    private val phoneSyncManager: PhoneSyncManager? = null,
) : WearRefresher {

    private var watchConnected = false

    init {
        phoneDataLayerAppHelper
            .connectedAndInstalledNodes
            .catch { Timber.e("${it.message}") }
            .onEach { nodes ->
                Timber.d("Connected nodes: ${nodes.joinToString()}")
                watchConnected = nodes.isNotEmpty()
                lastUpdate.update()
            }
            .launchIn(scope)

        // Start listening for sync events from watch
        phoneSyncManager?.startListening()
    }

    private val lastUpdate: DataStore<LastUpdate> by lazy {
        registry.protoDataStore<LastUpdate>(scope)
    }

    override suspend fun refresh() {
        if (watchConnected) {
            lastUpdate.update()
            // Push all tasks to watch for bidirectional sync
            try {
                phoneSyncManager?.sendTaskSnapshot()
            } catch (e: Exception) {
                Timber.e(e, "Failed to push tasks to watch on refresh")
            }
        }
    }

    /**
     * Notify watch about a specific task change.
     * This is called after task updates to sync changes to watch.
     */
    suspend fun notifyTaskChanged(taskId: Long) {
        if (watchConnected) {
            phoneSyncManager?.notifyTaskChanged(taskId)
        }
    }
}

private suspend fun DataStore<LastUpdate>.update() {
    updateData { it.copy { now = System.currentTimeMillis() } }
}