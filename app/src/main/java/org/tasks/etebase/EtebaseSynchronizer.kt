package org.tasks.etebase

import android.content.Context
import android.graphics.Color
import at.bitfire.ical4android.ICalendar.Companion.prodId
import com.etebase.client.Collection
import com.etebase.client.Item
import com.etesync.journalmanager.Exceptions
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.property.ProdId
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTaskContainer
import timber.log.Timber
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.inject.Inject

class EtebaseSynchronizer @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val caldavDao: CaldavDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskDeleter: TaskDeleter,
        private val inventory: Inventory,
        private val clientProvider: EtebaseClientProvider,
        private val iCal: iCalendar) {
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
        } catch (e: KeyManagementException) {
            setError(account, e.message)
        } catch (e: NoSuchAlgorithmException) {
            setError(account, e.message)
        } catch (e: Exceptions.HttpException) {
            setError(account, e.message)
        } catch (e: IntegrityException) {
            setError(account, e.message)
        } catch (e: VersionTooNewException) {
            setError(account, e.message)
        }
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class, Exceptions.HttpException::class, IntegrityException::class, VersionTooNewException::class)
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
                calendar = CaldavCalendar()
                calendar.name = meta.name
                calendar.account = account.uuid
                calendar.url = collection.uid
                calendar.uuid = UUIDHelper.newUUID()
                calendar.color = color
                caldavDao.insert(calendar)
            } else {
                if (calendar.name != meta.name || calendar.color != color) {
                    calendar.name = meta.name
                    calendar.color = color
                    caldavDao.update(calendar)
                    localBroadcastManager.broadcastRefreshList()
                }
            }
            sync(client, calendar, collection)
        }
        setError(account, "")
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        localBroadcastManager.broadcastRefreshList()
        if (!isNullOrEmpty(message)) {
            Timber.e(message)
        }
    }

    @Throws(IntegrityException::class, Exceptions.HttpException::class, VersionTooNewException::class)
    private suspend fun sync(
            client: EtebaseClient,
            caldavCalendar: CaldavCalendar,
            collection: Collection
    ) {
        Timber.d("sync(%s)", caldavCalendar)
        val localChanges = HashMap<String?, CaldavTaskContainer>()
        for (task in caldavDao.getCaldavTasksToPush(caldavCalendar.uuid!!)) {
            localChanges[task.remoteId] = task
        }
        val remoteCtag = collection.stoken
        if (isNullOrEmpty(remoteCtag) || remoteCtag != caldavCalendar.ctag) {
            Timber.d("${caldavCalendar.name}: Applying remote changes")
            client.fetchItems(collection, caldavCalendar) {
                applyEntries(caldavCalendar, it, localChanges.keys)
                client.updateCache(collection, it.second)
            }
        } else {
            Timber.d("${caldavCalendar.name} up to date")
        }
        val changes = ArrayList<Item>()
        for (caldavTask in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            client.deleteItem(collection, caldavTask)
                    ?.let { changes.add(it) }
                    ?: caldavDao.delete(caldavTask)
        }
        for (change in localChanges.values) {
            val task = change.task
            val caldavTask = change.caldavTask
            if (task.isDeleted) {
                client.deleteItem(collection, caldavTask)
                        ?.let { changes.add(it) }
                        ?: taskDeleter.delete(task)
            } else {
                changes.add(client.updateItem(
                        collection,
                        caldavTask,
                        iCal.toVtodo(caldavTask, task)
                ))
            }
        }
        if (changes.isNotEmpty()) {
            client.uploadChanges(collection, changes)
            applyEntries(caldavCalendar, Pair(caldavCalendar.ctag, changes), HashSet())
            client.updateCache(collection, changes)
        }
        Timber.d("UPDATE %s", caldavCalendar)
        caldavDao.update(caldavCalendar)
        caldavDao.updateParents(caldavCalendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun applyEntries(
            caldavCalendar: CaldavCalendar,
            items: Pair<String?, List<Item>>,
            dirty: MutableSet<String?>) {
        for (item in items.second) {
            val vtodo = item.contentString
            val task = fromVtodo(vtodo) ?: continue
            val remoteId = task.uid
            val caldavTask = caldavDao.getTaskByRemoteId(caldavCalendar.uuid!!, remoteId!!)
            if (item.isDeleted) {
                dirty.remove(remoteId)
                if (caldavTask != null) {
                    if (caldavTask.isDeleted()) {
                        caldavDao.delete(caldavTask)
                    } else {
                        taskDeleter.delete(caldavTask.task)
                    }
                }
            } else {
                caldavTask?.`object` = item.uid
                if (dirty.contains(remoteId)) {
                    caldavTask!!.vtodo = vtodo
                    caldavDao.update(caldavTask)
                } else {
                    iCal.fromVtodo(caldavCalendar, caldavTask, task, vtodo, item.uid, null)
                }
            }
        }
        caldavCalendar.ctag = items.first
        Timber.d("Setting stoken to ${caldavCalendar.ctag}")
        caldavDao.update(caldavCalendar)
    }
}