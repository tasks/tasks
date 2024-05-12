package org.tasks.etebase

import android.content.Context
import android.graphics.Color
import at.bitfire.ical4android.ICalendar.Companion.prodId
import com.etebase.client.Collection
import com.etebase.client.Item
import com.etebase.client.exceptions.ConnectionException
import com.etebase.client.exceptions.PermissionDeniedException
import com.etebase.client.exceptions.ServerErrorException
import com.etebase.client.exceptions.TemporaryServerErrorException
import com.etebase.client.exceptions.UnauthorizedException
import org.tasks.data.UUIDHelper
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.property.ProdId
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

class EtebaseSynchronizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskDeleter: TaskDeleter,
    private val inventory: Inventory,
    private val clientProvider: EtebaseClientProvider,
    private val iCal: iCalendar,
    private val vtodoCache: VtodoCache,
) {
    companion object {
        init {
            prodId = ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN")
        }
    }

    suspend fun sync(account: CaldavAccount) {
        Thread.currentThread().contextClassLoader = context.classLoader

        if (!inventory.hasPro) {
            setError(account, context.getString(R.string.requires_pro_subscription))
            return
        }
        if (isNullOrEmpty(account.password)) {
            setError(account, context.getString(R.string.password_required))
            return
        }
        try {
            synchronize(account)
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
        Timber.d("Found uids: %s", uids)
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, uids)) {
            taskDeleter.delete(calendar)
        }
        for (collection in collections) {
            val uid = collection.uid
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, uid)
            val meta = collection.meta
            val color = meta.color?.let { Color.parseColor(it) } ?: 0
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
                localBroadcastManager.broadcastRefreshList()
            }
            fetchChanges(account, client, calendar, collection)
            pushLocalChanges(account, client, calendar, collection)
        }
        setError(account, "")
    }

    private suspend fun setError(account: CaldavAccount, e: Throwable) =
            setError(account, e.message)

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        localBroadcastManager.broadcastRefreshList()
        if (!isNullOrEmpty(message)) {
            Timber.e(message)
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        client: EtebaseClient,
        caldavCalendar: CaldavCalendar,
        collection: Collection
    ) {
        if (caldavCalendar.ctag?.equals(collection.stoken) == true) {
            Timber.d("${caldavCalendar.name} up to date")
            return
        }
        Timber.d("updating $caldavCalendar")
        client.fetchItems(collection, caldavCalendar) { (stoken, items) ->
            applyEntries(account, caldavCalendar, items, stoken)
            client.updateCache(collection, items)
        }
        Timber.d("UPDATE %s", caldavCalendar)
        caldavDao.update(caldavCalendar)
        caldavDao.updateParents(caldavCalendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
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