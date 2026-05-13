package org.tasks.wear

import com.google.android.gms.wearable.MessageClient
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class WearRefresherImpl(
    phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
    private val messageClient: MessageClient,
    private val scope: CoroutineScope,
) : WearRefresher {

    private var connectedNodeIds: List<String> = emptyList()

    init {
        phoneDataLayerAppHelper
            .connectedAndInstalledNodes
            .catch { Timber.e("${it.message}") }
            .onEach { nodes ->
                connectedNodeIds = nodes.map { it.id }
                sendRefreshMessage()
            }
            .launchIn(scope)
    }

    override suspend fun refresh() {
        sendRefreshMessage()
    }

    private suspend fun sendRefreshMessage() {
        for (nodeId in connectedNodeIds) {
            try {
                messageClient.sendMessage(nodeId, PATH_REFRESH, byteArrayOf()).await()
            } catch (e: Exception) {
                Timber.e(e, "Failed to send refresh message to $nodeId")
            }
        }
    }

    companion object {
        const val PATH_REFRESH = "/tasks/refresh"
    }
}
