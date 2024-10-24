package org.tasks.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.AppInstallationStatus
import com.google.android.horologist.data.apphelper.AppInstallationStatusNodeType
import com.google.android.horologist.datalayer.watch.WearDataLayerAppHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.tasks.extensions.wearDataLayerRegistry

@OptIn(ExperimentalHorologistApi::class)
class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    fun installOnNode(nodeId: String?) = viewModelScope.launch {
        if (nodeId == null) {
            Log.d("MainActivityViewModel", "Missing nodeId")
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

    init {
        _uiState.value = NodesActionScreenState.Loading

        loadNodes()
    }

    fun loadNodes() = viewModelScope.launch {
        if (!helper.isAvailable()) {
            _uiState.value = NodesActionScreenState.ApiNotAvailable
        } else {
            _uiState.value = NodesActionScreenState.Loaded(
                nodeList = helper.connectedNodes().map { node ->
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
                },
            ).also {
                Log.d("MainActivityViewModel", "Loaded: $it")
            }
        }
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
    UNKNOWN
}

sealed class NodesActionScreenState {
    data object Idle : NodesActionScreenState()

    data object Loading : NodesActionScreenState()

    data class Loaded(val nodeList: List<NodeUiModel>) : NodesActionScreenState()

    data object ApiNotAvailable : NodesActionScreenState()
}
