package org.tasks.compose.chips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.throttleLatest
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CaldavListCache
import org.tasks.filters.TagFilter

class ChipDataProvider(
    private val caldavLists: CaldavListCache,
    tagDataDao: TagDataDao,
    private val refreshBroadcaster: RefreshBroadcaster,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var tagDatas: Map<String?, TagFilter> = emptyMap()

    var listsCount: Int by mutableStateOf(0)
        private set

    var tagsVersion: Int by mutableStateOf(0)
        private set

    fun getCaldavList(caldav: String?): CaldavFilter? =
        if (caldavLists.size <= 1) null else caldavLists.getByUuid(caldav)

    fun getTag(tag: String?): TagFilter? = tagDatas[tag]

    private fun updateTags(updated: List<TagData>) {
        val tags = updated.associateBy({ it.remoteId }) { TagFilter(it) }
        // Syncing writes to the tag tables constantly and every write lands here. Refreshing on
        // each one re-runs the task list query and rebuilds the whole list, repeatedly, for the
        // duration of a sync. Only a change to what a chip actually renders needs to reach the
        // list. Same gate as the one in CaldavListCache.
        val changed = tags.appearance() != tagDatas.appearance()
        tagDatas = tags
        if (changed) {
            Logger.d("ChipDataProvider") { "Updating tags" }
            tagsVersion++
            refreshBroadcaster.broadcastRefresh()
        }
    }

    private fun Map<String?, TagFilter>.appearance() =
        mapValues { (_, filter) -> Triple(filter.title, filter.icon, filter.tint) }

    init {
        caldavLists.updates
            .onEach {
                Logger.d("ChipDataProvider") { "Updating lists" }
                listsCount = caldavLists.size
                refreshBroadcaster.broadcastRefresh()
            }
            .launchIn(scope)
        tagDataDao
            .subscribeToTags()
            .throttleLatest(1000)
            .onEach { updateTags(it) }
            .launchIn(scope)
    }
}
