package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.compose.settings.PickerColor
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.compose.settings.buildPickerColors
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.filters.TagFilter
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.name_cannot_be_empty
import tasks.kmp.generated.resources.tag_already_exists

open class TagSettingsViewModel(
    private val tagDataDao: TagDataDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val reporting: Reporting,
    private val purchaseState: PurchaseState,
    private val tagMetadataSync: TagMetadataSync,
    private val syncAdapters: SyncAdapters,
    isDark: Boolean,
    hasColorWheel: Boolean = false,
    private val tagData: TagData,
) : ViewModel() {

    data class ViewState(
        val name: String = "",
        val color: Int = 0,
        val icon: String = TasksIcons.LABEL,
        val nameError: String? = null,
        val isLoading: Boolean = false,
        val showColorPicker: Boolean = false,
        val showIconPicker: Boolean = false,
        val pickerColors: List<PickerColor> = emptyList(),
        val hasPro: Boolean = false,
        val hasColorWheel: Boolean = false,
        val showDiscardDialog: Boolean = false,
    )

    val isNew: Boolean = tagData.id == null

    val tagFilter: TagFilter?
        get() = if (isNew) null else TagFilter(tagData)

    private val _viewState = MutableStateFlow(
        ViewState(
            name = tagData.name ?: "",
            color = tagData.color ?: 0,
            icon = tagData.icon ?: TasksIcons.LABEL,
            pickerColors = buildPickerColors(isDark),
            hasPro = purchaseState.purchasedThemes(),
            hasColorWheel = hasColorWheel,
        )
    )
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    val hasChanges: Boolean
        get() {
            val s = _viewState.value
            return if (isNew) {
                s.name.isNotBlank() || s.color != 0 || s.icon != TasksIcons.LABEL
            } else {
                s.name.trim() != tagData.name ||
                        s.color != (tagData.color ?: 0) ||
                        s.icon != (tagData.icon ?: TasksIcons.LABEL)
            }
        }

    fun setName(value: String) = _viewState.update { it.copy(name = value, nameError = null) }

    fun openColorPicker() = _viewState.update {
        it.copy(showColorPicker = true, hasPro = purchaseState.purchasedThemes())
    }

    fun closeColorPicker() = _viewState.update { it.copy(showColorPicker = false) }

    fun selectColor(color: Int) = _viewState.update { it.copy(color = color, showColorPicker = false) }

    fun setColor(color: Int) = _viewState.update { it.copy(color = color) }

    fun openIconPicker() = _viewState.update {
        it.copy(showIconPicker = true, hasPro = purchaseState.purchasedThemes())
    }

    fun closeIconPicker() = _viewState.update { it.copy(showIconPicker = false) }

    fun selectIcon(name: String) = _viewState.update { it.copy(icon = name, showIconPicker = false) }

    fun showDiscardDialog() = _viewState.update { it.copy(showDiscardDialog = true) }

    fun dismissDiscardDialog() = _viewState.update { it.copy(showDiscardDialog = false) }


    fun save(onDismiss: () -> Unit = {}, onComplete: (TagData) -> Unit) = withLoading {
        val name = _viewState.value.name.trim()
        if (!validate(name)) return@withLoading
        when {
            isNew -> create(name)?.let(onComplete)
            hasChanges -> update(name)?.let(onComplete)
            else -> onDismiss()
        }
    }

    fun persist(onComplete: (TagData) -> Unit) =
        save(onDismiss = { onComplete(tagData) }, onComplete = onComplete)

    private fun withLoading(block: suspend () -> Unit) {
        if (_viewState.value.isLoading) return
        _viewState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Logger.e(e) { "tag settings save failed" }
            } finally {
                _viewState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun validate(name: String): Boolean {
        if (name.isEmpty()) {
            _viewState.update { it.copy(nameError = getString(Res.string.name_cannot_be_empty)) }
            return false
        }
        if (clashes(name)) {
            _viewState.update { it.copy(nameError = getString(Res.string.tag_already_exists)) }
            return false
        }
        return true
    }

    private suspend fun clashes(newName: String): Boolean =
        (isNew || !newName.equals(tagData.name, ignoreCase = true)) &&
                tagDataDao.getTagByName(newName) != null

    private suspend fun create(name: String): TagData? = withContext(NonCancellable) {
        val s = _viewState.value
        val created = tagDataDao.createDirty(
            tagData.copy(name = name, color = s.color, icon = s.icon)
        ) ?: run {
            _viewState.update { it.copy(nameError = getString(Res.string.tag_already_exists)) }
            return@withContext null
        }
        reporting.logEvent(AnalyticsEvents.CREATE_TAG)
        refreshBroadcaster.broadcastRefresh()
        syncAdapters.sync(SyncSource.METADATA_CHANGE)
        created
    }

    private suspend fun update(name: String): TagData? = withContext(NonCancellable) {
        val s = _viewState.value
        val isRename = TagData.normalize(name) != tagData.normalizedName
        val nameChanged = name != tagData.name
        val colorChanged = s.color != (tagData.color ?: 0)
        val iconChanged = s.icon != (tagData.icon ?: TasksIcons.LABEL)
        val remoteId = tagData.remoteId!!
        if (isRename) {
            val row = tagMetadataSync.renameTag(remoteId, name, s.color, s.icon, colorChanged, iconChanged, tagData.order)
            if (row == null) {
                _viewState.update { it.copy(nameError = getString(Res.string.tag_already_exists)) }
                null
            } else {
                refreshBroadcaster.broadcastRefresh()
                syncAdapters.sync(SyncSource.METADATA_CHANGE)
                row
            }
        } else {
            if (!tagDataDao.editTag(remoteId, name, s.color, s.icon, nameChanged, colorChanged, iconChanged, tagData.order)) {
                _viewState.update { it.copy(nameError = getString(Res.string.tag_already_exists)) }
                return@withContext null
            }
            refreshBroadcaster.broadcastRefresh()
            syncAdapters.sync(SyncSource.METADATA_CHANGE)
            tagDataDao.getByUuid(remoteId)
        }
    }

    fun delete(onComplete: (String?) -> Unit) = withLoading {
        withContext(NonCancellable) {
            reporting.logEvent(
                AnalyticsEvents.SETTINGS_CLICK,
                AnalyticsEvents.PARAM_TYPE to AnalyticsEvents.SettingsClick.DELETE_TAG,
            )
            val uuid = tagData.remoteId
            tagMetadataSync.deleteTag(tagData)
            refreshBroadcaster.broadcastRefresh()
            syncAdapters.sync(SyncSource.METADATA_CHANGE)
            onComplete(uuid)
        }
    }
}
