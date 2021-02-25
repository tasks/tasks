package org.tasks.caldav

import android.content.Context
import at.bitfire.dav4jvm.DavCalendar
import at.bitfire.dav4jvm.DavCalendar.Companion.MIME_ICALENDAR
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.Response.HrefRelation
import at.bitfire.dav4jvm.exception.DavException
import at.bitfire.dav4jvm.exception.HttpException
import at.bitfire.dav4jvm.exception.ServiceUnavailableException
import at.bitfire.dav4jvm.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.*
import at.bitfire.dav4jvm.property.GetETag.Companion.fromResponse
import at.bitfire.ical4android.ICalendar.Companion.prodId
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.property.ProdId
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.property.Invite
import org.tasks.caldav.property.OCInvite
import org.tasks.caldav.property.OCOwnerPrincipal
import org.tasks.caldav.property.PropertyUtils.register
import org.tasks.caldav.property.ShareAccess
import org.tasks.caldav.property.ShareAccess.Companion.READ
import org.tasks.caldav.property.ShareAccess.Companion.READ_WRITE
import org.tasks.caldav.property.ShareAccess.Companion.SHARED_OWNER
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLException

class CaldavSynchronizer @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val caldavDao: CaldavDao,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskDeleter: TaskDeleter,
        private val inventory: Inventory,
        private val firebase: Firebase,
        private val provider: CaldavClientProvider,
        private val iCal: iCalendar) {

    suspend fun sync(account: CaldavAccount) {
        Thread.currentThread().contextClassLoader = context.classLoader

        if (!inventory.hasPro && !account.isTasksOrg) {
            setError(account, context.getString(R.string.requires_pro_subscription))
            return
        }
        if (isNullOrEmpty(account.password)) {
            setError(account, if (account.isTasksOrg) {
                ERROR_UNAUTHORIZED
            } else {
                context.getString(R.string.password_required)
            })
            return
        }
        try {
            synchronize(account)
        } catch (e: SocketTimeoutException) {
            setError(account, e.message)
        } catch (e: SSLException) {
            setError(account, e.message)
        } catch (e: ConnectException) {
            setError(account, e.message)
        } catch (e: UnknownHostException) {
            setError(account, e.message)
        } catch (e: UnauthorizedException) {
            setError(account, e.message)
        } catch (e: ServiceUnavailableException) {
            setError(account, e.message)
        } catch (e: KeyManagementException) {
            setError(account, e.message)
        } catch (e: NoSuchAlgorithmException) {
            setError(account, e.message)
        } catch (e: IOException) {
            setError(account, e.message)
        } catch (e: HttpException) {
            val message = when(e.code) {
                402, in 500..599 -> e.message
                else -> {
                    firebase.reportException(e)
                    e.message
                }
            }
            setError(account, message)
        } catch (e: Exception) {
            setError(account, e.message)
            firebase.reportException(e)
        }
    }

    @Throws(IOException::class, DavException::class, KeyManagementException::class, NoSuchAlgorithmException::class)
    private suspend fun synchronize(account: CaldavAccount) {
        val caldavClient = provider.forAccount(account)
        val resources = caldavClient.calendars()
        val urls = resources.map { it.href.toString() }.toHashSet()
        Timber.d("Found calendars: %s", urls)
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, ArrayList(urls))) {
            taskDeleter.delete(calendar)
        }
        for (resource in resources) {
            val url = resource.href.toString()
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, url)
            val remoteName = resource[DisplayName::class.java]!!.displayName
            val calendarColor = resource[CalendarColor::class.java]
            val access = resource.accessLevel
            if (access == ACCESS_UNKNOWN) {
                firebase.logEvent(
                    R.string.event_sync_unknown_access,
                    R.string.param_type to
                            (resource[ShareAccess::class.java]?.access?.toString() ?: "???")
                )
            }
            val color = calendarColor?.color ?: 0
            if (calendar == null) {
                calendar = CaldavCalendar()
                calendar.name = remoteName
                calendar.account = account.uuid
                calendar.url = url
                calendar.uuid = UUIDHelper.newUUID()
                calendar.color = color
                calendar.access = access
                caldavDao.insert(calendar)
            } else if (calendar.name != remoteName
                    || calendar.color != color
                    || calendar.access != access
            ) {
                calendar.color = color
                calendar.name = remoteName
                calendar.access = access
                caldavDao.update(calendar)
                localBroadcastManager.broadcastRefreshList()
            }
            sync(calendar, resource, caldavClient.httpClient)
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

    @Throws(DavException::class)
    private suspend fun sync(
            caldavCalendar: CaldavCalendar,
            resource: Response,
            httpClient: OkHttpClient) {
        Timber.d("sync(%s)", caldavCalendar)
        val httpUrl = resource.href
        if (caldavCalendar.access != ACCESS_READ_ONLY) {
            pushLocalChanges(caldavCalendar, httpClient, httpUrl)
        }
        val remoteCtag = resource.ctag
        if (caldavCalendar.ctag?.equals(remoteCtag) == true) {
            Timber.d("%s up to date", caldavCalendar.name)
            return
        }
        val davCalendar = DavCalendar(httpClient, httpUrl)
        val members = ArrayList<Response>()
        davCalendar.calendarQuery("VTODO", null, null) { response, relation ->
            if (relation == HrefRelation.MEMBER) {
                members.add(response)
            }
        }
        val changed = members.filter { vCard: Response ->
            val eTag = vCard[GetETag::class.java]?.eTag
            if (eTag.isNullOrBlank()) {
                return@filter false
            }
            eTag != caldavDao.getTask(caldavCalendar.uuid!!, vCard.hrefName())?.etag
        }
        for (items in changed.chunked(30)) {
            val urls = items.map { it.href }
            val responses = ArrayList<Response>()
            davCalendar.multiget(urls) { response, relation ->
                if (relation == HrefRelation.MEMBER) {
                    responses.add(response)
                }
            }
            Timber.d("MULTI %s", urls)
            for (vCard in responses) {
                val eTag = vCard[GetETag::class.java]?.eTag
                val url = vCard.href
                if (eTag.isNullOrBlank()) {
                    throw DavException("Received CalDAV GET response without ETag for $url")
                }
                val vtodo = vCard[CalendarData::class.java]?.iCalendar
                if (vtodo.isNullOrBlank()) {
                    throw DavException("Received CalDAV GET response without CalendarData for $url")
                }
                val fileName = vCard.hrefName()
                val remote = fromVtodo(vtodo)
                if (remote == null) {
                    Timber.e("Invalid VCALENDAR: %s", fileName)
                    return
                }
                val caldavTask = caldavDao.getTask(caldavCalendar.uuid!!, fileName)
                iCal.fromVtodo(caldavCalendar, caldavTask, remote, vtodo, fileName, eTag)
            }
        }
        caldavDao
                .getObjects(caldavCalendar.uuid!!)
                .subtract(members.map { it.hrefName() })
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Timber.d("DELETED $it")
                    taskDeleter.delete(caldavDao.getTasks(caldavCalendar.uuid!!, it.toList()))
                }
        caldavCalendar.ctag = remoteCtag
        Timber.d("UPDATE %s", caldavCalendar)
        caldavDao.update(caldavCalendar)
        caldavDao.updateParents(caldavCalendar.uuid!!)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun pushLocalChanges(
            caldavCalendar: CaldavCalendar, httpClient: OkHttpClient, httpUrl: HttpUrl) {
        for (task in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            deleteRemoteResource(httpClient, httpUrl, task)
        }
        for (task in taskDao.getCaldavTasksToPush(caldavCalendar.uuid!!)) {
            try {
                pushTask(task, httpClient, httpUrl)
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    private suspend fun deleteRemoteResource(
            httpClient: OkHttpClient, httpUrl: HttpUrl, caldavTask: CaldavTask): Boolean {
        try {
            if (!isNullOrEmpty(caldavTask.`object`)) {
                val remote = DavResource(
                        httpClient, httpUrl.newBuilder().addPathSegment(caldavTask.`object`!!).build())
                remote.delete(null) {}
            }
        } catch (e: HttpException) {
            if (e.code != 404) {
                Timber.e(e)
                return false
            }
        } catch (e: IOException) {
            Timber.e(e)
            return false
        }
        caldavDao.delete(caldavTask)
        return true
    }

    @Throws(IOException::class)
    private suspend fun pushTask(task: Task, httpClient: OkHttpClient, httpUrl: HttpUrl) {
        Timber.d("pushing %s", task)
        val caldavTask = caldavDao.getTask(task.id) ?: return
        if (task.isDeleted) {
            if (deleteRemoteResource(httpClient, httpUrl, caldavTask)) {
                taskDeleter.delete(task)
            }
            return
        }
        val data = iCal.toVtodo(caldavTask, task)
        val requestBody = RequestBody.create(MIME_ICALENDAR, data)
        try {
            val remote = DavResource(
                    httpClient, httpUrl.newBuilder().addPathSegment(caldavTask.`object`!!).build())
            remote.put(requestBody) {
                val getETag = fromResponse(it)
                if (getETag != null && !isNullOrEmpty(getETag.eTag)) {
                    caldavTask.etag = getETag.eTag
                    caldavTask.vtodo = String(data)
                }
            }
        } catch (e: HttpException) {
            Timber.e(e)
            return
        }
        caldavTask.lastSync = task.modificationDate
        caldavDao.update(caldavTask)
        Timber.d("SENT %s", caldavTask)
    }

    companion object {
        init {
            prodId = ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN")
        }

        fun registerFactories() {
            PropertyRegistry.register(
                ShareAccess.Factory(),
                Invite.Factory(),
                OCOwnerPrincipal.Factory(),
                OCInvite.Factory(),
            )
        }

        val Response.ctag: String?
            get() = this[SyncToken::class.java]?.token ?: this[GetCTag::class.java]?.cTag

        val Response.accessLevel: Int
            get() {
                this[ShareAccess::class.java]?.let {
                    return when (it.access) {
                        SHARED_OWNER -> ACCESS_OWNER
                        READ_WRITE -> ACCESS_READ_WRITE
                        READ -> ACCESS_READ_ONLY
                        else -> ACCESS_UNKNOWN
                    }
                }
                this[OCOwnerPrincipal::class.java]?.owner?.let {
                    val current = this[CurrentUserPrincipal::class.java]?.href
                    if (current?.endsWith("$it/") == true) {
                        return ACCESS_OWNER
                    }
                }
                return when (this[CurrentUserPrivilegeSet::class.java]?.mayWriteContent) {
                    false -> ACCESS_READ_ONLY
                    else -> ACCESS_READ_WRITE
                }
        }
    }
}