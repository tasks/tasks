package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.themes.TasksIcons

/**
 * The four reorderable drawer section types, mirroring the native app's
 * "Customize drawer" screen (NavigationDrawerCustomization), which reorders
 * custom filters, tags, places and lists and commits `0..n` per section via
 * the matching DAO.
 */
enum class DrawerItemType { LIST, TAG, PLACE, FILTER }

/**
 * One reorderable drawer row. [key] is namespaced so ids from different
 * tables (lists/tags/places/filters all auto-increment from 1) can never
 * collide in the reorderable library's key space.
 */
data class DrawerCustomizationItem(
    val type: DrawerItemType,
    val id: Long,
    val name: String,
    val order: Int = NO_ORDER,
    val count: Int? = null,
    val icon: String? = null,
) {
    val key: String get() = "${type.name.lowercase()}-$id"
}

/**
 * One drawer section. Sections come straight from
 * [FilterProvider.drawerCustomizationItems], so the screen shows exactly the
 * sections the drawer itself shows (custom filters, tags, places, and one
 * section per account for lists), already in drawer order.
 */
data class DrawerCustomizationSection(
    val id: String,
    val title: String,
    val items: ImmutableList<DrawerCustomizationItem> = persistentListOf(),
)

data class DrawerCustomizationState(
    val loaded: Boolean = false,
    val hasManualOrder: Boolean = false,
    val sections: ImmutableList<DrawerCustomizationSection> = persistentListOf(),
)

/**
 * Backs the desktop "Customize drawer" screen (settings -> Navigation
 * drawer), the compose counterpart of the native app's
 * NavigationDrawerCustomization activity.
 *
 * Sections are read through [FilterProvider.drawerCustomizationItems], the
 * same data source the native screen uses; drag moves are applied in memory
 * per section ([onMove], same-section only, like the native
 * ItemTouchHelperCallback) and committed on release ([onDragStopped]) by
 * rewriting the section's `order` column as `0..n` via the matching DAO.
 * Tag order is additionally pushed to the server through the metadata sync
 * adapter, same as native. Reset restores every section to alphabetical
 * (`order = -1`).
 *
 * Ordering is device-local, with the one native-parity exception of tag
 * order being synced.
 *
 * Suppressed lints: the constructor takes one parameter per injected
 * dependency, matching the plain-constructor DI style used across the
 * other view models in this package; the `catch (e: Exception)` blocks
 * match the same fault-tolerance convention used in the neighbouring
 * view models.
 */
@Suppress("LongParameterList")
open class DrawerCustomizationViewModel(
    private val filterProvider: FilterProvider,
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val locationDao: LocationDao,
    private val filterDao: FilterDao,
    private val tagMetadataSync: TagMetadataSync,
    private val syncAdapters: SyncAdapters,
    private val refreshBroadcaster: RefreshBroadcaster,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawerCustomizationState())
    val state = _state.asStateFlow()

    private var lastMovedSectionId: String? = null

    init {
        reload()
    }

    @Suppress("TooGenericExceptionCaught") // Repo convention: broad catch, log and mark loaded.
    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sections = mutableListOf<DrawerCustomizationSection>()
                var lastSectionIndex = -1
                filterProvider.drawerCustomizationItems(forceExpand = true).forEach { item ->
                    when (item) {
                        is NavigationDrawerSubheader -> {
                            sections.add(DrawerCustomizationSection(id = item.id, title = item.title.orEmpty()))
                            lastSectionIndex = sections.lastIndex
                        }
                        is Filter -> item.toCustomizationItem()?.let { customizationItem ->
                            // Filters attach to the most recent subheader section
                            // (the drawer never emits a filter before its header).
                            val index = lastSectionIndex
                            if (index >= 0) {
                                val section = sections[index]
                                sections[index] = section.copy(
                                    items = (section.items + customizationItem).toPersistentList(),
                                )
                            }
                        }
                        else -> Unit
                    }
                }
                _state.update {
                    it.copy(
                        loaded = true,
                        sections = sections.toPersistentList(),
                        hasManualOrder = sections.any { section ->
                            section.items.any { it.order != NO_ORDER }
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to load drawer customization" }
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    private fun Filter.toCustomizationItem(): DrawerCustomizationItem? {
        // null for items the customization screen does not show
        // (built-in and debug filters, the sign-in prompt).
        return when (this) {
        is CaldavFilter -> DrawerCustomizationItem(
            type = DrawerItemType.LIST,
            id = calendar.id,
            name = calendar.name.orEmpty(),
            order = calendar.order,
            count = count.takeIf { it >= 0 },
            icon = calendar.icon?.takeIf { it.isNotBlank() },
        )
        is TagFilter -> tagData.id?.let { id ->
            DrawerCustomizationItem(
                type = DrawerItemType.TAG,
                id = id,
                name = tagData.name.orEmpty(),
                order = tagData.order,
                count = count.takeIf { it >= 0 },
                icon = tagData.icon?.takeIf { it.isNotBlank() } ?: TasksIcons.LABEL,
            )
        }
        is PlaceFilter -> DrawerCustomizationItem(
            type = DrawerItemType.PLACE,
            id = place.id,
            name = title,
            order = place.order,
            count = count.takeIf { it >= 0 },
            icon = place.icon?.takeIf { it.isNotBlank() } ?: TasksIcons.PLACE,
        )
        is CustomFilter -> DrawerCustomizationItem(
            type = DrawerItemType.FILTER,
            id = id,
            name = title,
            order = order,
            count = null,
            icon = filter.icon?.takeIf { it.isNotBlank() } ?: TasksIcons.FILTER_LIST,
        )
        else -> null
        }
    }

    /**
     * In-flight reorder called by the reorderable library's move callback as
     * a drag crosses rows. Keys are the namespaced item keys (see
     * [DrawerCustomizationItem.key]). A move is only applied inside one
     * section; anything else is a no-op, which the library interprets as
     * "stay put". Sections are homogeneous by construction, so this also
     * enforces the native rules (same type, same account for lists).
     */
    fun onMove(fromKey: Any, toKey: Any) {
        if (fromKey !is String || toKey !is String || fromKey == toKey) return
        _state.update { current -> moveWithinSection(current, fromKey, toKey) }
    }

    private fun moveWithinSection(
        current: DrawerCustomizationState,
        fromKey: String,
        toKey: String,
    ): DrawerCustomizationState {
        var movedSectionId: String? = null
        val sections = current.sections.map { section ->
            val movedItems = moveItems(section.items, fromKey, toKey)
            if (movedItems == null) {
                section
            } else {
                movedSectionId = section.id
                section.copy(items = movedItems)
            }
        }.toPersistentList()
        if (movedSectionId == null) {
            return current
        }
        lastMovedSectionId = movedSectionId
        return current.copy(sections = sections)
    }

    // Reorder [fromKey] to where [toKey] sits within one section's items.
    // Returns the new list, or null when the move is a no-op (a key is not
    // in this section, or the item did not actually change position).
    private fun moveItems(
        items: ImmutableList<DrawerCustomizationItem>,
        fromKey: String,
        toKey: String,
    ): ImmutableList<DrawerCustomizationItem>? {
        val list = items.toMutableList()
        val fromIndex = list.indexOfFirst { it.key == fromKey }
        val toIndex = list.indexOfFirst { it.key == toKey }
        if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) {
            return null
        }
        list.add(toIndex, list.removeAt(fromIndex))
        return list.toPersistentList()
    }

    /**
     * Commit the settled order of the section the drag ended in: rewrite
     * `order` as `0..n` across the section (same non-transactional TODO as
     * the native screen) and refresh so the drawer re-sorts live.
     */
    fun onDragStopped() {
        val sectionId = lastMovedSectionId ?: return
        lastMovedSectionId = null
        viewModelScope.launch(Dispatchers.IO) {
            commitSectionOrder(sectionId)
        }
    }

    @Suppress("TooGenericExceptionCaught") // Repo convention: broad catch, log and swallow.
    private suspend fun commitSectionOrder(sectionId: String) {
        try {
            val section = _state.value.sections.firstOrNull { it.id == sectionId } ?: return
            section.items.forEachIndexed { order, item ->
                setOrder(item, order)
            }
            if (section.items.any { it.type == DrawerItemType.TAG }) {
                pushTagOrder()
            }
            _state.update { it.copy(hasManualOrder = true) }
            refreshBroadcaster.broadcastRefresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to commit drawer order" }
        }
    }

    /** Restore every section to alphabetical order (`order = -1`), like the native reset action. */
    @Suppress("TooGenericExceptionCaught") // Repo convention: broad catch, log and swallow.
    fun resetOrder() {
        lastMovedSectionId = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                filterDao.resetOrders()
                caldavDao.resetOrders()
                tagDataDao.resetOrders()
                locationDao.resetOrders()
                pushTagOrder()
                refreshBroadcaster.broadcastRefresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to reset drawer order" }
            }
            reload()
        }
    }

    private suspend fun setOrder(item: DrawerCustomizationItem, order: Int) {
        when (item.type) {
            DrawerItemType.LIST -> caldavDao.setOrder(item.id, order)
            DrawerItemType.TAG -> tagDataDao.setOrder(item.id, order)
            DrawerItemType.PLACE -> locationDao.setOrder(item.id, order)
            DrawerItemType.FILTER -> filterDao.setOrder(item.id, order)
        }
    }

    @Suppress("TooGenericExceptionCaught") // Repo convention: broad catch, log and swallow.
    private suspend fun pushTagOrder() {
        // Mirrors the native screen: mark the metadata order dirty and
        // trigger a metadata sync.
        try {
            tagMetadataSync.markOrderDirty()
            syncAdapters.sync(SyncSource.METADATA_CHANGE)
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "failed to queue tag order push" }
        }
    }

    companion object {
        private const val TAG = "DrawerCustomizationViewModel"
    }
}
