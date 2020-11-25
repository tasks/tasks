package org.tasks.etebase

import android.content.Context
import androidx.core.util.Pair
import at.bitfire.ical4android.ICalendar.Companion.prodId
import com.etesync.journalmanager.Exceptions
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import com.etesync.journalmanager.JournalEntryManager
import com.etesync.journalmanager.JournalEntryManager.Entry.Companion.getFakeWithUid
import com.etesync.journalmanager.JournalManager.Journal
import com.etesync.journalmanager.UserInfoManager
import com.etesync.journalmanager.model.SyncEntry
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
import kotlin.collections.HashSet

class EteBaseSynchronizer @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val caldavDao: CaldavDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskDeleter: TaskDeleter,
        private val inventory: Inventory,
        private val clientProvider: EteBaseClientProvider,
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
        if (isNullOrEmpty(account.encryptionKey)) {
            setError(account, context.getString(R.string.encryption_password_required))
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
        val userInfo = client.userInfo()
        val resources = client.getCalendars(userInfo)
        val uids: Set<String> = resources.values.mapNotNull { it.uid }.toHashSet()
        Timber.d("Found uids: %s", uids)
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, uids.toList())) {
            taskDeleter.delete(calendar)
        }
        for ((key, collection) in resources) {
            val uid = collection.uid
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, uid!!)
            val colorInt = collection.color
            val color = colorInt ?: 0
            if (calendar == null) {
                calendar = CaldavCalendar()
                calendar.name = collection.displayName
                calendar.account = account.uuid
                calendar.url = collection.uid
                calendar.uuid = UUIDHelper.newUUID()
                calendar.color = color
                caldavDao.insert(calendar)
            } else {
                if (calendar.name != collection.displayName
                        || calendar.color != color) {
                    calendar.name = collection.displayName
                    calendar.color = color
                    caldavDao.update(calendar)
                    localBroadcastManager.broadcastRefreshList()
                }
            }
            sync(client, userInfo!!, calendar, key)
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
            client: EteBaseClient,
            userInfo: UserInfoManager.UserInfo,
            caldavCalendar: CaldavCalendar,
            journal: Journal) {
        Timber.d("sync(%s)", caldavCalendar)
        val localChanges = HashMap<String?, CaldavTaskContainer>()
        for (task in caldavDao.getCaldavTasksToPush(caldavCalendar.uuid!!)) {
            localChanges[task.remoteId] = task
        }
        var remoteCtag = journal.lastUid
        if (isNullOrEmpty(remoteCtag) || remoteCtag != caldavCalendar.ctag) {
            Timber.v("Applying remote changes")
            client.getSyncEntries(userInfo, journal, caldavCalendar) {
                applyEntries(caldavCalendar, it, localChanges.keys)
            }
        } else {
            Timber.d("%s up to date", caldavCalendar.name)
        }
        val changes: MutableList<SyncEntry> = ArrayList()
        for (task in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            val vtodo = task.vtodo
            if (!isNullOrEmpty(vtodo)) {
                changes.add(SyncEntry(vtodo!!, SyncEntry.Actions.DELETE))
            }
        }
        for (task in localChanges.values) {
            val vtodo = task.vtodo
            val existingTask = !isNullOrEmpty(vtodo)
            if (task.isDeleted) {
                if (existingTask) {
                    changes.add(SyncEntry(vtodo!!, SyncEntry.Actions.DELETE))
                }
            } else {
                changes.add(
                        SyncEntry(
                                String(iCal.toVtodo(task.caldavTask, task.task)),
                                if (existingTask) SyncEntry.Actions.CHANGE else SyncEntry.Actions.ADD))
            }
        }
        remoteCtag = caldavCalendar.ctag
        val crypto = client.getCrypto(userInfo, journal)
        val updates: MutableList<Pair<JournalEntryManager.Entry, SyncEntry>> = ArrayList()
        var previous: JournalEntryManager.Entry? = if (isNullOrEmpty(remoteCtag)) null else getFakeWithUid(remoteCtag!!)
        for (syncEntry in changes) {
            val entry = JournalEntryManager.Entry()
            entry.update(crypto, syncEntry.toJson(), previous)
            updates.add(Pair.create(entry, syncEntry))
            previous = entry
        }
        if (updates.size > 0) {
            Timber.v("Pushing local changes")
            client.pushEntries(journal, updates.mapNotNull { it.first }, remoteCtag)
            Timber.v("Applying local changes")
            applyEntries(caldavCalendar, updates, HashSet())
        }
        Timber.d("UPDATE %s", caldavCalendar)
        caldavDao.update(caldavCalendar)
        caldavDao.updateParents(caldavCalendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun applyEntries(
            caldavCalendar: CaldavCalendar,
            syncEntries: List<Pair<JournalEntryManager.Entry, SyncEntry>>,
            dirty: MutableSet<String?>) {
        for (entry in syncEntries) {
            val journalEntry = entry.first
            val syncEntry = entry.second
            val action = syncEntry!!.action
            val vtodo = syncEntry.content
            Timber.v("%s: %s", action, vtodo)
            val task = fromVtodo(vtodo) ?: continue
            val remoteId = task.uid
            val caldavTask = caldavDao.getTaskByRemoteId(caldavCalendar.uuid!!, remoteId!!)
            when (action) {
                SyncEntry.Actions.ADD, SyncEntry.Actions.CHANGE -> if (dirty.contains(remoteId)) {
                    caldavTask!!.vtodo = vtodo
                    caldavDao.update(caldavTask)
                } else {
                    iCal.fromVtodo(caldavCalendar, caldavTask, task, vtodo, null, null)
                }
                SyncEntry.Actions.DELETE -> {
                    dirty.remove(remoteId)
                    if (caldavTask != null) {
                        if (caldavTask.isDeleted()) {
                            caldavDao.delete(caldavTask)
                        } else {
                            taskDeleter.delete(caldavTask.task)
                        }
                    }
                }
            }
            caldavCalendar.ctag = journalEntry!!.uid
            caldavDao.update(caldavCalendar)
        }
    }
}