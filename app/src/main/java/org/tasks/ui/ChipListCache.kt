package org.tasks.ui

import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import org.tasks.LocalBroadcastManager
import org.tasks.data.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChipListCache @Inject internal constructor(
        googleTaskListDao: GoogleTaskListDao,
        caldavDao: CaldavDao,
        tagDataDao: TagDataDao,
        private val localBroadcastManager: LocalBroadcastManager) {

    private val googleTaskLists: MutableMap<String?, GtasksFilter> = HashMap()
    private val caldavCalendars: MutableMap<String?, CaldavFilter> = HashMap()
    private val tagDatas: MutableMap<String?, TagFilter> = HashMap()
    private fun updateGoogleTaskLists(updated: List<GoogleTaskList>) {
        googleTaskLists.clear()
        for (update in updated) {
            googleTaskLists[update.remoteId] = GtasksFilter(update)
        }
        localBroadcastManager.broadcastRefresh()
    }

    private fun updateCaldavCalendars(updated: List<CaldavCalendar>) {
        caldavCalendars.clear()
        for (update in updated) {
            caldavCalendars[update.uuid] = CaldavFilter(update)
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

    fun getGoogleTaskList(googleTaskList: String?): Filter? = googleTaskLists[googleTaskList]

    fun getCaldavList(caldav: String?): Filter? = caldavCalendars[caldav]

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    init {
        googleTaskListDao.subscribeToLists().observeForever { updated: List<GoogleTaskList> -> updateGoogleTaskLists(updated) }
        caldavDao.subscribeToCalendars().observeForever { updated: List<CaldavCalendar> -> updateCaldavCalendars(updated) }
        tagDataDao.subscribeToTags().observeForever { updated: List<TagData> -> updateTags(updated) }
    }
}