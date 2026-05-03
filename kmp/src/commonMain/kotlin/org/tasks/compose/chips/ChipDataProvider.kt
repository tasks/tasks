package org.tasks.compose.chips

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
import org.tasks.filters.TagFilter

class ChipDataProvider(
    caldavDao: CaldavDao,
    tagDataDao: TagDataDao,
    private val refreshBroadcaster: RefreshBroadcaster,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lists: MutableMap<String?, CaldavFilter> = HashMap()
    private val tagDatas: MutableMap<String?, TagFilter> = HashMap()

    @Volatile
    var listsCount: Int = 0
        private set

    fun getCaldavList(caldav: String?): CaldavFilter? =
        if (lists.size <= 1) null else lists[caldav]

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    private fun updateCaldavCalendars(
        accounts: List<CaldavAccount>,
        calendars: List<CaldavCalendar>,
    ) {
        Logger.d("ChipDataProvider") { "Updating lists" }
        calendars
            .mapNotNull { list ->
                val account = accounts.find { it.uuid == list.account } ?: return@mapNotNull null
                CaldavFilter(calendar = list, account = account)
            }
            .let {
                lists.clear()
                it.associateByTo(lists) { filter -> filter.uuid }
            }
        listsCount = lists.size
        refreshBroadcaster.broadcastRefresh()
    }

    private fun updateTags(updated: List<TagData>) {
        Logger.d("ChipDataProvider") { "Updating tags" }
        tagDatas.clear()
        for (update in updated) {
            tagDatas[update.remoteId] = TagFilter(update)
        }
        refreshBroadcaster.broadcastRefresh()
    }

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
