package org.tasks.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.TagFilter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChipListCache @Inject internal constructor(
    caldavDao: CaldavDao,
    tagDataDao: TagDataDao,
    private val localBroadcastManager: LocalBroadcastManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lists: MutableMap<String?, CaldavFilter> = HashMap()
    private val tagDatas: MutableMap<String?, TagFilter> = HashMap()
    private fun updateCaldavCalendars(
        accounts: List<CaldavAccount>,
        calendars: List<CaldavCalendar>
    ) {
        Timber.d("Updating lists")
        calendars
            .mapNotNull { list ->
                val account = accounts.find { it.uuid == list.account } ?: return@mapNotNull null
                CaldavFilter(calendar = list, account = account)
            }
            .let {
                lists.clear()
                it.associateByTo(lists) { filter -> filter.uuid }
            }
        localBroadcastManager.broadcastRefresh()
    }

    private fun updateTags(updated: List<TagData>) {
        tagDatas.clear()
        for (update in updated) {
            tagDatas[update.remoteId] = TagFilter(update)
        }
        localBroadcastManager.broadcastRefresh()
    }

    fun getCaldavList(caldav: String?): CaldavFilter? = lists[caldav]

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    init {
        caldavDao
            .watchAccounts()
            .combine(caldavDao.subscribeToCalendars()) { accounts, calendars ->
                updateCaldavCalendars(accounts, calendars)
            }
            .launchIn(scope)
        tagDataDao.subscribeToTags().onEach { updateTags(it) }.launchIn(scope)
    }
}