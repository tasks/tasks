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
                byUuid = filters.associateBy { it.uuid }
                byId = filters.associateBy { it.calendar.id }
                _version.value++
            }
            .launchIn(scope)
    }
}
