/**
 * OfflineSettingsViewModel.kt — ViewModel for the Wear OS settings screen.
 *
 * ## Responsibilities
 * - Reads settings from [SettingsRepository] (Room-backed).
 * - Polls phone connectivity via [NodeClient] every 5 seconds.
 * - Combines settings + connectivity + sync state into
 *   [OfflineSettingsViewState] exposed as [viewState].
 * - Provides write methods ([setShowHidden], [setShowCompleted], etc.)
 *   that persist to Room and attempt a sync when connected.
 *
 * ## Data flow
 * ```
 * SettingsEntity (Flow)        ─┐
 * isConnected (MutableState)   ─┤── combine ──▶ OfflineSettingsViewState
 * syncState (StateFlow)        ─┘
 * ```
 */
package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tasks.data.local.SettingsEntity
import org.tasks.data.local.SettingsRepository
import org.tasks.data.sync.DataLayerSyncManager
import org.tasks.data.sync.SyncState
import timber.log.Timber

/**
 * UI state for the offline-capable settings screen.
 */
data class OfflineSettingsViewState(
    val initialized: Boolean = false,
    val showHidden: Boolean = false,
    val showCompleted: Boolean = false,
    val filter: String = "",
    val collapsedGroups: Set<Long> = emptySet(),
    val isConnected: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
)

/**
 * ViewModel for settings that works offline.
 * Stores settings locally and syncs with phone when connected.
 */
class OfflineSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val syncManager = DataLayerSyncManager.getInstance(application)
    private val nodeClient: NodeClient = Wearable.getNodeClient(application)

    private val _isConnected = MutableStateFlow(false)

    val viewState: StateFlow<OfflineSettingsViewState> = combine(
        settingsRepository.getSettings(),
        _isConnected,
        syncManager.syncState,
    ) { settings, connected, syncState ->
        OfflineSettingsViewState(
            initialized = true,
            showHidden = settings.showHidden,
            showCompleted = settings.showCompleted,
            filter = settings.filter,
            collapsedGroups = settings.collapsedGroups
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toLongOrNull() }
                .toSet(),
            isConnected = connected,
            syncState = syncState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfflineSettingsViewState()
    )

    init {
        // Ensure settings exist
        viewModelScope.launch {
            settingsRepository.ensureSettingsExist()
        }

        // Check connection status periodically
        checkConnectionStatus()

        // Start sync manager listener
        syncManager.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stopListening()
    }

    /**
     * Check if phone is connected using NodeClient.
     */
    private fun checkConnectionStatus() {
        viewModelScope.launch {
            // Check periodically
            while (true) {
                try {
                    val connectedNodes = nodeClient.connectedNodes.await()
                    _isConnected.value = connectedNodes.isNotEmpty()
                    Timber.d("Phone connection status: ${_isConnected.value}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to check connection status")
                    _isConnected.value = false
                }
                // Check every 5 seconds
                delay(5000)
            }
        }
    }

    /**
     * Set show hidden setting (works offline).
     */
    fun setShowHidden(showHidden: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowHidden(showHidden)
            trySyncSettings()
        }
    }

    /**
     * Set show completed setting (works offline).
     */
    fun setShowCompleted(showCompleted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowCompleted(showCompleted)
            trySyncSettings()
        }
    }

    /**
     * Set filter (works offline).
     */
    fun setFilter(filter: String) {
        viewModelScope.launch {
            settingsRepository.setFilter(filter)
            trySyncSettings()
        }
    }

    /**
     * Toggle group collapsed state (works offline).
     */
    fun toggleGroup(groupId: Long, collapsed: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleGroupCollapsed(groupId, collapsed)
            trySyncSettings()
        }
    }

    /**
     * Try to sync settings with phone if connected.
     */
    private suspend fun trySyncSettings() {
        if (_isConnected.value) {
            try {
                syncManager.processPendingOps()
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync settings")
            }
        }
    }

    /**
     * Force refresh connection status and sync.
     */
    fun refresh() {
        checkConnectionStatus()
        viewModelScope.launch {
            syncManager.processPendingOps()
        }
    }
}
