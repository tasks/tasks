package org.tasks.ui

import com.todoroo.astrid.api.TagFilter
import org.tasks.LocalBroadcastManager
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.TagData
import org.tasks.data.dao.TagDataDao
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
        caldavDao.subscribeToCalendars().observeForever { updated: List<CaldavCalendar> -> updateCaldavCalendars(updated) }
        tagDataDao.subscribeToTags().observeForever { updated: List<TagData> -> updateTags(updated) }
    }
}