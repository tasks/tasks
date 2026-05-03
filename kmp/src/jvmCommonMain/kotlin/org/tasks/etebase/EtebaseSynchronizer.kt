package org.tasks.etebase

import co.touchlab.kermit.Logger
import com.etebase.client.Collection
import com.etebase.client.Item
import com.etebase.client.exceptions.ConnectionException
import com.etebase.client.exceptions.PermissionDeniedException
import com.etebase.client.exceptions.ServerErrorException
import com.etebase.client.exceptions.TemporaryServerErrorException
import com.etebase.client.exceptions.UnauthorizedException
import net.fortuna.ical4j.model.property.ProdId
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.AnalyticsEvents.INITIAL_SYNC_COMPLETE
import org.tasks.analytics.AnalyticsEvents.PARAM_TASK_COUNT
import org.tasks.analytics.AnalyticsEvents.PARAM_TYPE
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.Task.Companion.prodId
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.kmp.PROD_ID
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.requires_pro_subscription

class EtebaseSynchronizer(
    private val caldavDao: CaldavDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val clientProvider: EtebaseClientProvider,
    private val iCal: iCalendar,
    private val vtodoCache: VtodoCache,
    private val reporting: Reporting,
) {
    companion object {
        init {
            prodId = ProdId(PROD_ID)
        }
    }

    suspend fun sync(account: CaldavAccount, hasPro: Boolean) {
        Logger.d("EtebaseSynchronizer") { "Synchronizing $account" }
        if (!hasPro) {
            setError(account, getString(Res.string.requires_pro_subscription))
            return
        }
        if (account.password.isNullOrEmpty()) {
            setError(account, getString(Res.string.password_required))
            return
        }
        try {
            synchronize(account)
            if (account.lastSync == 0L) {
                val taskCount = caldavDao.getTaskCountForAccount(account.uuid!!)
                reporting.logEvent(
                    INITIAL_SYNC_COMPLETE,
                    PARAM_TYPE to Constants.SYNC_TYPE_ETEBASE,
                    PARAM_TASK_COUNT to taskCount
                )
            }
            account.lastSync = currentTimeMillis()
            setError(account, "")
        } catch (e: ConnectionException) {
            setError(account, e)
        } catch (e: PermissionDeniedException) {
            setError(account, e)
        } catch (e: ServerErrorException) {
            setError(account, e)
        } catch (e: TemporaryServerErrorException) {
            setError(account, e)
        } catch (e: UnauthorizedException) {
            setError(account, e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount) {
        val client = clientProvider.forAccount(account)
        val collections = client.getCollections()
        val uids = collections.map { it.uid }
        Logger.d("EtebaseSynchronizer") { "Found uids: $uids" }
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, uids)) {
            taskDeleter.delete(calendar)
        }
        for (collection in collections) {
            val uid = collection.uid
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, uid)
            val meta = collection.meta
            val color = meta.color
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    val parsed = java.lang.Long.decode(it).toInt()
                    if (it.length == 7) parsed or 0xFF000000.toInt() else parsed
                }
                ?: 0
            if (calendar == null) {
                calendar = CaldavCalendar(
                    name = meta.name,
                    account = account.uuid,
                    url = collection.uid,
                    uuid = UUIDHelper.newUUID(),
                    color = color,
                )
                caldavDao.insert(calendar)
            } else if (calendar.name != meta.name || calendar.color != color) {
                calendar.name = meta.name
                calendar.color = color
                caldavDao.update(calendar)
                refreshBroadcaster.broadcastRefresh()
            }
            fetchChanges(account, client, calendar, collection)
            pushLocalChanges(account, client, calendar, collection)
        }
    }

    private suspend fun setError(account: CaldavAccount, e: Throwable) =
            setError(account, e.message)

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrEmpty()) {
            Logger.e("EtebaseSynchronizer") { message }
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        client: EtebaseClient,
        caldavCalendar: CaldavCalendar,
        collection: Collection
    ) {
        if (caldavCalendar.ctag?.equals(collection.stoken) == true) {
            Logger.d("EtebaseSynchronizer") { "${caldavCalendar.name} up to date" }
            return
        }
        Logger.d("EtebaseSynchronizer") { "updating $caldavCalendar" }
        client.fetchItems(collection, caldavCalendar) { (stoken, items) ->
            applyEntries(account, caldavCalendar, items, stoken)
            client.updateCache(collection, items)
        }
        Logger.d("EtebaseSynchronizer") { "UPDATE $caldavCalendar" }
        caldavDao.update(caldavCalendar)
        Logger.d("EtebaseSynchronizer") { "Updating parents for ${caldavCalendar.uuid}" }
        caldavDao.updateParents(caldavCalendar.uuid!!)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        client: EtebaseClient,
        caldavCalendar: CaldavCalendar,
        collection: Collection
    ) {
        val changes = ArrayList<Item>()
        for (caldavTask in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            client.deleteItem(collection, caldavTask)
                    ?.let { changes.add(it) }
                    ?: run {
                        vtodoCache.delete(caldavCalendar, caldavTask)
                        caldavDao.delete(caldavTask)
                    }
        }
        for (change in caldavDao.getCaldavTasksToPush(caldavCalendar.uuid!!)) {
            val task = change.task
            val caldavTask = change.caldavTask
            caldavTask.lastSync = task.modificationDate
            if (task.isDeleted) {
                client.deleteItem(collection, caldavTask)
                        ?.let { changes.add(it) }
                        ?: taskDeleter.delete(task)
            } else {
                changes.add(
                        client.updateItem(
                            collection,
                            caldavTask,
                            iCal.toVtodo(account, caldavCalendar, caldavTask, task)
                        )
                )
            }
        }
        if (changes.isNotEmpty()) {
            client.uploadChanges(collection, changes)
            applyEntries(account, caldavCalendar, changes, isLocalChange = true)
            client.updateCache(collection, changes)
        }
    }

    private suspend fun applyEntries(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        items: List<Item>,
        stoken: String? = null,
        isLocalChange: Boolean = false
    ) {
        for (item in items) {
            val vtodo = item.contentString
            val task = fromVtodo(vtodo) ?: continue
            val remoteId = task.uid
            val caldavTask = caldavDao.getTaskByRemoteId(caldavCalendar.uuid!!, remoteId!!)
            if (item.isDeleted) {
                if (caldavTask != null) {
                    if (caldavTask.isDeleted()) {
                        vtodoCache.delete(caldavCalendar, caldavTask)
                        caldavDao.delete(caldavTask)
                    } else {
                        taskDeleter.delete(caldavTask.task)
                    }
                }
            } else if (isLocalChange) {
                caldavTask?.let {
                    vtodoCache.putVtodo(caldavCalendar, it, vtodo)
                    it.lastSync = item.meta.mtime ?: currentTimeMillis()
                    caldavDao.update(it)
                }
            } else {
                caldavTask?.obj = item.uid
                iCal.fromVtodo(account, caldavCalendar, caldavTask, task, vtodo, item.uid, null)
            }
        }
        stoken?.let {
            caldavCalendar.ctag = it
            caldavDao.update(caldavCalendar)
        }
    }
}
