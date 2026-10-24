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
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.AlphanumComparator

/**
 * One draggable list row: a CalDAV calendar (task list).
 *
 * [calendarId] is the stable item key for the reorderable list; it is what
 * [ListOrderViewModel.onMove] and the commit path look items up by.
 */
data class ListOrderItem(
    val calendarId: Long,
    val name: String,
    val order: Int = NO_ORDER,
)

/**
 * One account section. Lists can only be reordered within their own section,
 * matching the drawer (lists are grouped under their account) and the Android
 * customization screen (moves across section types or accounts are refused).
 */
data class ListOrderSection(
    val accountUuid: String,
    val accountName: String,
    val lists: ImmutableList<ListOrderItem> = persistentListOf(),
)

data class ListOrderState(
    val loaded: Boolean = false,
    val hasManualOrder: Boolean = false,
    val sections: ImmutableList<ListOrderSection> = persistentListOf(),
)

/**
 * Backs the desktop "List order" settings screen: drag-reorder the CalDAV
 * lists inside each account section.
 *
 * The drawer and the "group by list" section order both already honour
 * `caldav_lists.cdl_order` (manual order first, alphanum name as fallback), so
 * this view model only needs to read the sections and write `cdl_order` on
 * drop. [onMove] updates the in-memory order while the drag is in flight
 * (the reorderable library calls it once per row crossed); [onDragStopped]
 * commits the settled section by rewriting `cdl_order` as `0..n`, exactly like
 * Android's NavigationDrawerCustomization does, and broadcasts a refresh so
 * the drawer re-sorts live.
 *
 * List order is device-local on CalDAV (same as on Android) - it is never
 * pushed to the server.
 */
open class ListOrderViewModel(
    private val caldavDao: CaldavDao,
    private val refreshBroadcaster: RefreshBroadcaster,
) : ViewModel() {

    private val _state = MutableStateFlow(ListOrderState())
    val state = _state.asStateFlow()

    /** The section touched by the most recent [onMove], or null if the drag made no move. */
    private var pendingSectionUuid: String? = null

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sections = caldavDao.getAccounts()
                    .mapNotNull { account ->
                        val uuid = account.uuid ?: return@mapNotNull null
                        val lists = caldavDao.getCaldavFilters(uuid)
                            .map {
                                ListOrderItem(
                                    calendarId = it.caldavCalendar.id,
                                    name = it.caldavCalendar.name.orEmpty(),
                                    order = it.caldavCalendar.order,
                                )
                            }
                            .sortedWith(LIST_ORDER)
                            .toPersistentList()
                        if (lists.isEmpty()) return@mapNotNull null
                        ListOrderSection(
                            accountUuid = uuid,
                            accountName = account.name.orEmpty(),
                            lists = lists,
                        )
                    }
                    .toPersistentList()
                _state.update {
                    it.copy(
                        loaded = true,
                        sections = sections,
                        hasManualOrder = hasManualOrderInDb(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to load list order" }
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    private suspend fun hasManualOrderInDb(): Boolean =
        caldavDao.getAccounts().any { account ->
            val uuid = account.uuid ?: return@any false
            caldavDao.getCaldavFilters(uuid).any { it.caldavCalendar.order != NO_ORDER }
        }

    /**
     * In-flight reorder called by the reorderable library's move callback as a
     * drag crosses rows. Keys are the LazyColumn item keys: [Long] calendarIds
     * for list rows and [String] header keys for account headers. A move is
     * only applied inside one section; anything else (cross-section, header)
     * is a no-op, which the library interprets as "stay put".
     */
    fun onMove(fromKey: Any, toKey: Any) {
        if (fromKey !is Long || toKey !is Long || fromKey == toKey) return
        _state.update { current ->
            val section = current.sections.firstOrNull { it.lists.any { l -> l.calendarId == fromKey } }
                ?: return@update current
            if (section.lists.none { it.calendarId == toKey }) return@update current
            val lists = section.lists.toMutableList()
            val fromIndex = lists.indexOfFirst { it.calendarId == fromKey }
            val toIndex = lists.indexOfFirst { it.calendarId == toKey }
            if (fromIndex == toIndex) return@update current
            lists.add(toIndex, lists.removeAt(fromIndex))
            pendingSectionUuid = section.accountUuid
            current.copy(
                sections = current.sections
                    .map { s ->
                        if (s.accountUuid == section.accountUuid) s.copy(lists = lists.toPersistentList()) else s
                    }
                    .toPersistentList(),
            )
        }
    }

    /**
     * Commit the settled order of the section the drag ended in: rewrite
     * `cdl_order` as `0..n` across the section and refresh so the drawer and
     * any "group by list" view re-sort. The rewrite is not in one transaction
     * (same TODO as Android's screen) - a crash mid-rewrite still leaves a
     * consistent total order, just possibly truncated.
     */
    fun onDragStopped() {
        val uuid = pendingSectionUuid ?: return
        pendingSectionUuid = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val section = _state.value.sections.firstOrNull { it.accountUuid == uuid }
                    ?: return@launch
                section.lists.forEachIndexed { order, item ->
                    caldavDao.setOrder(item.calendarId, order)
                }
                _state.update { it.copy(hasManualOrder = true) }
                refreshBroadcaster.broadcastRefresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to commit list order" }
            }
        }
    }

    /** Restore every list to alphabetical order (`cdl_order = -1`), like Android's Reset action. */
    fun resetOrder() {
        pendingSectionUuid = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                caldavDao.resetOrders()
                refreshBroadcaster.broadcastRefresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to reset list order" }
            }
            reload()
        }
    }

    companion object {
        private const val TAG = "ListOrderViewModel"

        /**
         * Mirror of the drawer's list sort (FilterProvider.COMPARATOR): manually
         * ordered lists first, by order value, then the rest alphanumerically.
         */
        private val LIST_ORDER = Comparator<ListOrderItem> { a, b ->
            when {
                a.order == NO_ORDER && b.order == NO_ORDER ->
                    ALPHANUM.compare(a, b)
                a.order == NO_ORDER -> 1
                b.order == NO_ORDER -> -1
                a.order < b.order -> -1
                a.order > b.order -> 1
                else -> ALPHANUM.compare(a, b)
            }
        }

        private val ALPHANUM = AlphanumComparator<ListOrderItem>(ListOrderItem::name)
    }
}
