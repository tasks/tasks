package org.tasks.ui

import org.tasks.filters.TagFilter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChipListCache @Inject internal constructor(
    caldavDao: CaldavDao,
    tagDataDao: TagDataDao,
    private val localBroadcastManager: LocalBroadcastManager) {

    private val caldavCalendars: MutableMap<String?, CaldavCalendar> = HashMap()
    private val tagDatas: MutableMap<String?, TagFilter> = HashMap()
    private fun updateCaldavCalendars(updated: List<CaldavCalendar>) {
        caldavCalendars.clear()
        for (update in updated) {
            caldavCalendars[update.uuid] = update
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

    fun getCaldavList(caldav: String?): CaldavCalendar? = caldavCalendars[caldav]

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    init {
        caldavDao.subscribeToCalendars().onEach { updateCaldavCalendars(it) }.launchIn(GlobalScope)
        tagDataDao.subscribeToTags().onEach { updateTags(it) }.launchIn(GlobalScope)
    }
}