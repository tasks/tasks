package org.tasks.compose.chips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.throttleLatest
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.TagFilter

class ChipDataProvider(
    caldavDao: CaldavDao,
    tagDataDao: TagDataDao,
    private val refreshBroadcaster: RefreshBroadcaster,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lists: Map<String?, CaldavFilter> = emptyMap()

    @Volatile
    private var tagDatas: Map<String?, TagFilter> = emptyMap()

    var listsCount: Int by mutableStateOf(0)
        private set

    var tagsVersion: Int by mutableStateOf(0)
        private set

    fun getCaldavList(caldav: String?): CaldavFilter? =
        if (lists.size <= 1) null else lists[caldav]

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    private fun updateCaldavCalendars(
        accounts: List<CaldavAccount>,
        calendars: List<CaldavCalendar>,
    ) {
        val updated: Map<String?, CaldavFilter> = calendars
            .mapNotNull { list ->
                val account = accounts.find { it.uuid == list.account } ?: return@mapNotNull null
                CaldavFilter(calendar = list, account = account)
            }
            .associateBy { filter -> filter.uuid }
        val changed = updated.chipAppearance() != lists.chipAppearance()
        lists = updated
        listsCount = updated.size
        if (changed) {
            Logger.d("ChipDataProvider") { "Updating lists" }
            refreshBroadcaster.broadcastRefresh()
        }
    }

    private fun updateTags(updated: List<TagData>) {
        val tags = updated.associateBy({ it.remoteId }) { TagFilter(it) }
        val changed = tags.chipAppearance() != tagDatas.chipAppearance()
        tagDatas = tags
        if (changed) {
            Logger.d("ChipDataProvider") { "Updating tags" }
            tagsVersion++
            refreshBroadcaster.broadcastRefresh()
        }
    }

    /**
     * Syncing writes to these tables constantly - sync tokens, ctags, error state, ordering - and
     * every write emits here. Broadcasting a refresh on each one re-ran the task list query and
     * rebuilt the whole list repeatedly for the duration of a sync. Only a change to what a chip
     * actually renders needs to reach the list.
     */
    private fun <K> Map<K, Filter>.chipAppearance() =
        mapValues { (_, filter) -> Triple(filter.title, filter.icon, filter.tint) }

    init {
        combine(caldavDao.watchAccounts(), caldavDao.subscribeToCalendars()) { accounts, calendars ->
            accounts to calendars
        }
            .throttleLatest(1000)
            .onEach { (accounts, calendars) -> updateCaldavCalendars(accounts, calendars) }
            .launchIn(scope)
        tagDataDao
            .subscribeToTags()
            .throttleLatest(1000)
            .onEach { updateTags(it) }
            .launchIn(scope)
    }
}
