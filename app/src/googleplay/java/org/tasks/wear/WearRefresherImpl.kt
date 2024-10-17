package org.tasks.wear

import androidx.datastore.core.DataStore
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
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
) : WearRefresher {

    private var watchConnected = false

    init {
        phoneDataLayerAppHelper
            .connectedAndInstalledNodes
            .onEach { nodes ->
                Timber.d("Connected nodes: ${nodes.joinToString()}")
                watchConnected = nodes.isNotEmpty()
            }
            .launchIn(scope)
    }

    private val lastUpdate: DataStore<LastUpdate> by lazy {
        registry.protoDataStore<LastUpdate>(scope)
    }

    override suspend fun refresh() {
        if (watchConnected) {
            lastUpdate.updateData {
                it.copy {
                    now = System.currentTimeMillis()
                }
            }
        }
    }
}