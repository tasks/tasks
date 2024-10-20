package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.GrpcProto.Settings
import org.tasks.SettingsKt
import org.tasks.WearServiceGrpcKt
import org.tasks.copy
import org.tasks.extensions.wearDataLayerRegistry

data class ViewState(
    val initialized: Boolean = false,
    val settings: Settings = Settings.getDefaultInstance(),
)

@OptIn(ExperimentalHorologistApi::class)
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val registry = application.wearDataLayerRegistry(viewModelScope)
    private val wearService: WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
        nodeId = TargetNodeId.PairedPhone,
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    init {
        registry
            .protoFlow<Settings>(TargetNodeId.PairedPhone)
            .onEach { newSettings ->
                _viewState.update {
                    it.copy(
                        initialized = true,
                        settings = newSettings,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setShowHidden(showHidden: Boolean) {
        updateSettings {
            this.showHidden = showHidden
        }
    }

    fun setShowCompleted(showCompleted: Boolean) {
        updateSettings {
            this.showCompleted = showCompleted
        }
    }

    private fun updateSettings(block: SettingsKt.Dsl.() -> Unit) = viewModelScope.launch {
        wearService.updateSettings(
            GrpcProto.UpdateSettingsRequest.newBuilder()
                .setSettings(
                    _viewState.value.settings.copy {
                        block()
                    }
                )
                .build()
        )
    }
}