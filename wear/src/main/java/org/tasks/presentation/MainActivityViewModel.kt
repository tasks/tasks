package org.tasks.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.AppInstallationStatus
import com.google.android.horologist.data.apphelper.AppInstallationStatusNodeType
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import com.google.android.horologist.datalayer.watch.WearDataLayerAppHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.tasks.GrpcProto.GetVersionRequest
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    fun installOnNode(nodeId: String?) = viewModelScope.launch {
        if (nodeId == null) {
            Timber.d("Missing nodeId")
        } else {
            helper.installOnNode(nodeId)
        }
    }

    private val _uiState =
        MutableStateFlow<NodesActionScreenState>(NodesActionScreenState.Idle)
    val uiState = _uiState.asStateFlow()
    private val registry: WearDataLayerRegistry by lazy {
        application.wearDataLayerRegistry(viewModelScope)
    }
    private val helper: WearDataLayerAppHelper by lazy {
        WearDataLayerAppHelper(
            context = application,
            registry = registry,
            scope = viewModelScope,
        )
    }
    private var wearService: WearServiceGrpcKt.WearServiceCoroutineStub? = null

    private fun getOrCreateWearService(): WearServiceGrpcKt.WearServiceCoroutineStub {
        return wearService ?: registry.grpcClient(
            nodeId = getApplication<Application>().phoneTargetNodeId(),
            coroutineScope = viewModelScope,
        ) {
            WearServiceGrpcKt.WearServiceCoroutineStub(it)
        }.also { wearService = it }
    }

    init {
        _uiState.value = NodesActionScreenState.Loading

        loadNodes()
    }

    fun loadNodes() = viewModelScope.launch {
        if (!helper.isAvailable()) {
            _uiState.value = NodesActionScreenState.ApiNotAvailable
        } else {
            val nodeList = helper.connectedNodes().map { node ->
                val type = when (node.appInstallationStatus) {
                    is AppInstallationStatus.Installed -> {
                        val status =
                            node.appInstallationStatus as AppInstallationStatus.Installed
                        when (status.nodeType) {
                            AppInstallationStatusNodeType.WATCH -> NodeTypeUiModel.WATCH
                            AppInstallationStatusNodeType.PHONE -> NodeTypeUiModel.PHONE
                        }
                    }

                    AppInstallationStatus.NotInstalled -> NodeTypeUiModel.UNKNOWN
                }

                NodeUiModel(
                    id = node.id,
                    name = node.displayName,
                    appInstalled = node.appInstallationStatus is AppInstallationStatus.Installed,
                    type = type,
                )
            }
            val phoneNode = nodeList
                .firstOrNull { it.type == NodeTypeUiModel.PHONE && it.appInstalled }
            if (phoneNode != null) {
                val app = getApplication<Application>()
                app.savePhoneNodeId(phoneNode.id)
                val cachedVersion = app.getCachedPhoneVersion(phoneNode.id)
                val phoneUpdateRequired = if (cachedVersion >= MIN_PHONE_VERSION) {
                    false
                } else {
                    try {
                        val version = getOrCreateWearService()
                            .getVersion(GetVersionRequest.getDefaultInstance())
                        app.setCachedPhoneVersion(phoneNode.id, version.versionCode)
                        version.versionCode < MIN_PHONE_VERSION
                    } catch (e: Exception) {
                        Timber.e(e)
                        true
                    }
                }
                _uiState.value = NodesActionScreenState.Loaded(
                    nodeList = nodeList,
                    phoneUpdateRequired = phoneUpdateRequired,
                ).also {
                    Timber.d("Loaded: $it")
                }
            } else {
                getApplication<Application>().clearPhoneNodeId()
                _uiState.value = NodesActionScreenState.Loaded(nodeList = nodeList).also {
                    Timber.d("Loaded: $it")
                }
            }
        }
    }

    companion object {
        const val MIN_PHONE_VERSION = 141500
    }
}

data class NodeUiModel(
    val id: String,
    val name: String,
    val appInstalled: Boolean,
    val type: NodeTypeUiModel,
)

enum class NodeTypeUiModel {
    WATCH,
    PHONE,
    UNKNOWN,
}

enum class Errors {
    APP_NOT_INSTALLED,
    PHONE_UPDATE_REQUIRED,
    UNKNOWN
}

sealed class NodesActionScreenState {
    data object Idle : NodesActionScreenState()

    data object Loading : NodesActionScreenState()

    data class Loaded(
        val nodeList: List<NodeUiModel>,
        val phoneUpdateRequired: Boolean = false,
    ) : NodesActionScreenState()

    data object ApiNotAvailable : NodesActionScreenState()
}
