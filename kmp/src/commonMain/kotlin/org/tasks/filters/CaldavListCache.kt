package org.tasks.filters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tasks.compose.throttleLatest
import org.tasks.data.dao.CaldavDao

class CaldavListCache(
    private val caldavDao: CaldavDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var byUuid: Map<String?, CaldavFilter> = emptyMap()

    @Volatile
    private var byId: Map<Long, CaldavFilter> = emptyMap()

    private val _version = MutableStateFlow(0)

    val updates: Flow<Int> = _version.filter { it > 0 }

    val size: Int
        get() = byUuid.size

    fun getByUuid(uuid: String?): CaldavFilter? = byUuid[uuid]

    suspend fun getListTitle(id: Long): String? =
        byId[id]?.title?.takeIf { it.isNotBlank() }
            ?: caldavDao.getCalendarById(id)?.name?.takeIf { it.isNotBlank() }

    init {
        combine(caldavDao.watchAccounts(), caldavDao.subscribeToCalendars()) { accounts, calendars ->
            accounts to calendars
        }
            .throttleLatest(1000)
            .onEach { (accounts, calendars) ->
                val filters = calendars.mapNotNull { list ->
                    val account = accounts.find { it.uuid == list.account } ?: return@mapNotNull null
                    CaldavFilter(calendar = list, account = account)
                }
                val updated: Map<String?, CaldavFilter> = filters.associateBy { it.uuid }
                // Syncing writes to the calendar and account tables constantly - sync tokens,
                // ctags, error state - and every write reaches here. Bumping the version on each
                // one re-runs the task list query and rebuilds the whole list, repeatedly, for
                // the duration of a sync. The cache itself is still refreshed either way; only a
                // change to what a list actually renders as needs to reach its subscribers.
                val changed = updated.appearance() != byUuid.appearance()
                byUuid = updated
                byId = filters.associateBy { it.calendar.id }
                if (changed) {
                    _version.value++
                }
            }
            .launchIn(scope)
    }

    private fun Map<String?, CaldavFilter>.appearance() =
        mapValues { (_, filter) -> Triple(filter.title, filter.icon, filter.tint) }
}
