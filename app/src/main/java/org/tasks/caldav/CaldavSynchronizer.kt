package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.okhttp.DavCalendar
import at.bitfire.dav4jvm.okhttp.DavCalendar.Companion.MIME_ICALENDAR
import at.bitfire.dav4jvm.okhttp.DavResource
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.okhttp.Response.HrefRelation
import at.bitfire.dav4jvm.okhttp.exception.DavException
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import at.bitfire.dav4jvm.okhttp.exception.ServiceUnavailableException
import at.bitfire.dav4jvm.okhttp.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.caldav.CalendarColor
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrincipal
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrivilegeSet
import at.bitfire.dav4jvm.property.caldav.GetCTag
import at.bitfire.dav4jvm.property.webdav.DisplayName
import at.bitfire.dav4jvm.property.webdav.GetETag
import at.bitfire.dav4jvm.property.webdav.GetETag.Companion.fromResponse
import at.bitfire.dav4jvm.property.webdav.SyncToken
import org.tasks.service.TaskDeleter
import org.tasks.data.dao.TaskDao
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.property.ProdId
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.compose.resources.getString
import org.tasks.kmp.PROD_ID
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.requires_pro_subscription
import org.tasks.analytics.AnalyticsEvents.INITIAL_SYNC_COMPLETE
import org.tasks.analytics.AnalyticsEvents.PARAM_TASK_COUNT
import org.tasks.analytics.AnalyticsEvents.PARAM_TYPE
import org.tasks.analytics.AnalyticsEvents.SYNC_UNKNOWN_ACCESS
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.billing.Inventory
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.property.CalendarIcon
import org.tasks.caldav.property.Invite
import org.tasks.caldav.property.OCAccess
import org.tasks.caldav.property.OCInvite
import org.tasks.caldav.property.OCOwnerPrincipal
import org.tasks.caldav.property.OCUser
import org.tasks.caldav.property.ShareAccess
import org.tasks.caldav.property.ShareAccess.Companion.NOT_SHARED
import org.tasks.caldav.property.ShareAccess.Companion.NO_ACCESS
import org.tasks.caldav.property.ShareAccess.Companion.READ
import org.tasks.caldav.property.ShareAccess.Companion.READ_WRITE
import org.tasks.caldav.property.ShareAccess.Companion.SHARED_OWNER
import org.tasks.caldav.property.Sharee
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_ACCEPTED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_DECLINED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_INVALID
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.PrincipalAccess
import org.tasks.data.entity.Task
import timber.log.Timber
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.inject.Inject

class CaldavSynchronizer @Inject constructor(
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val inventory: Inventory,
    private val reporting: Reporting,
    private val provider: CaldavClientProvider,
    private val iCal: iCalendar,
    private val principalDao: PrincipalDao,
    private val vtodoCache: VtodoCache,
    private val accountDataRepository: TasksAccountDataRepository,
) {
    suspend fun sync(account: CaldavAccount) {
        Timber.d("Synchronizing $account")
        if (!inventory.hasPro && !account.isTasksOrg) {
            setError(account, getString(Res.string.requires_pro_subscription))
            return
        }
        if (account.password.isNullOrBlank()) {
            setError(account, if (account.isTasksOrg) {
                ERROR_UNAUTHORIZED
            } else {
                getString(Res.string.password_required)
            })
            return
        }
        try {
            synchronize(account)
            if (account.lastSync == 0L) {
                val taskCount = caldavDao.getTaskCountForAccount(account.uuid!!)
                val syncType = if (account.isTasksOrg) Constants.SYNC_TYPE_TASKS_ORG else Constants.SYNC_TYPE_CALDAV
                reporting.logEvent(
                    INITIAL_SYNC_COMPLETE,
                    PARAM_TYPE to syncType,
                    PARAM_TASK_COUNT to taskCount
                )
            }
            account.lastSync = currentTimeMillis()
            setError(account, "")
        } catch (e: IOException) {
            setError(account, e)
        } catch (e: UnauthorizedException) {
            setError(account, e)
        } catch (e: ServiceUnavailableException) {
            setError(account, e)
        } catch (e: KeyManagementException) {
            setError(account, e)
        } catch (e: NoSuchAlgorithmException) {
            setError(account, e)
        } catch (e: HttpException) {
            when(e.statusCode) {
                402, 451, in 500..599 -> {}
                else -> { reporting.reportException(e) }
            }
            setError(account, e)
        } catch (e: Exception) {
            setError(account, e)
            reporting.reportException(e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount) {
        val caldavClient = provider.forAccount(account)
        var serverType = account.serverType

        // Check guest status for Tasks.org accounts
        val isGuest = if (account.isTasksOrg) {
            try {
                accountDataRepository.fetchAndCache(account)?.guest ?: false
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch account data")
                accountDataRepository.getAccountResponse()?.guest ?: false
            }
        } else false

        val resources = caldavClient.calendars { response ->
            if (serverType == SERVER_UNKNOWN) {
                serverType = getServerType(account, response.headers)
            }
            response
        }
        if (serverType != account.serverType) {
            account.serverType = serverType
            caldavDao.update(account)
        }
        val urls = resources.map { it.href.toString() }.toHashSet()
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, ArrayList(urls))) {
            taskDeleter.delete(calendar)
        }
        for (resource in resources) {
            val url = resource.href.toString()
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, url)
            val remoteName = resource[DisplayName::class.java]!!.displayName
            val color = resource[CalendarColor::class.java]?.color ?: 0
            val rawAccess = resource.accessLevel
            val guestOwned = isGuest && rawAccess == ACCESS_OWNER
            val access = if (guestOwned) ACCESS_READ_ONLY else rawAccess
            val icon = resource[CalendarIcon::class.java]?.icon?.takeIf { it.isNotBlank() }

            if (rawAccess == ACCESS_UNKNOWN) {
                reporting.logEvent(
                    SYNC_UNKNOWN_ACCESS,
                    PARAM_TYPE to
                            (resource[ShareAccess::class.java]?.access?.toString() ?: "???")
                )
            }
            if (calendar == null) {
                calendar = CaldavCalendar(
                    name = remoteName,
                    account = account.uuid,
                    url = url,
                    uuid = UUIDHelper.newUUID(),
                    color = color,
                    access = access,
                    icon = icon,
                )
                caldavDao.insert(calendar)
            } else if (calendar.name != remoteName
                || calendar.color != color
                || calendar.access != access
                || (icon != null && calendar.icon != icon)
            ) {
                calendar = calendar.copy(
                    color = color,
                    name = remoteName,
                    access = access,
                    icon = icon ?: calendar.icon,
                )
                caldavDao.update(calendar)
                refreshBroadcaster.broadcastRefresh()
            }
            resource
                .principals(account, calendar)
                .let { principalDao.deleteRemoved(calendar.id, it.map(PrincipalAccess::id)) }
            fetchChanges(account, calendar, resource, caldavClient.httpClient)
            when {
                guestOwned -> pushLocalChanges(
                    account, calendar, caldavClient.httpClient, resource.href,
                    deleteOnly = true
                )
                calendar.access != ACCESS_READ_ONLY -> pushLocalChanges(
                    account, calendar, caldavClient.httpClient, resource.href
                )
            }
        }
    }

    private fun getServerType(account: CaldavAccount, headers: Headers) = when {
        account.isTasksOrg -> SERVER_TASKS
        headers["DAV"]?.contains("oc-resource-sharing") == true ->
            if (headers["DAV"]?.let { it.contains("nextcloud-") || it.contains("nc-") } == true)
                SERVER_NEXTCLOUD
            else
                SERVER_OWNCLOUD
        headers["x-sabre-version"]?.isNotBlank() == true -> SERVER_SABREDAV
        headers["server"] == "Openexchange WebDAV" -> SERVER_OPEN_XCHANGE
        else -> SERVER_UNKNOWN
    }

    private suspend fun setError(account: CaldavAccount, throwable: Throwable) {
        Timber.e(throwable, "$account: ${throwable.message}")
        setError(account, throwable.message)
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        if (!message.isNullOrBlank()) {
            Timber.e("$account: $message")
        }
        account.error = message
        caldavDao.update(account)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrBlank()) {
            Timber.e(message)
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        resource: Response,
        httpClient: OkHttpClient
    ) {
        val httpUrl = resource.href
        val remoteCtag = resource.ctag
        if (caldavCalendar.ctag?.equals(remoteCtag) == true) {
            Timber.d("up to date: $caldavCalendar")
            return
        }
        Timber.d("updating $caldavCalendar")
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
                iCal.fromVtodo(account, caldavCalendar, caldavTask, remote, vtodo, fileName, eTag)
            }
        }
        caldavDao
                .getRemoteObjects(caldavCalendar.uuid!!)
                .subtract(members.map { it.hrefName() })
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Timber.d("DELETED $it")
                    val tasks = caldavDao.getTasks(caldavCalendar.uuid!!, it.toList())
                    vtodoCache.delete(caldavCalendar, tasks)
                    taskDeleter.delete(tasks.map { it.task })
                }
        caldavCalendar.ctag = remoteCtag
        Timber.d("UPDATE %s", caldavCalendar)
        caldavDao.update(caldavCalendar)
        Timber.d("Updating parents for ${caldavCalendar.uuid}")
        caldavDao.updateParents(caldavCalendar.uuid!!)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        httpClient: OkHttpClient,
        httpUrl: HttpUrl,
        deleteOnly: Boolean = false,
    ) {
        for (task in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            deleteRemoteResource(httpClient, httpUrl, caldavCalendar, task)
        }
        for (task in taskDao.getCaldavTasksToPush(caldavCalendar.uuid!!)) {
            if (deleteOnly && !task.isDeleted) continue
            try {
                pushTask(account, caldavCalendar, task, httpClient, httpUrl)
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    private suspend fun deleteRemoteResource(
        httpClient: OkHttpClient,
        httpUrl: HttpUrl,
        calendar: CaldavCalendar,
        caldavTask: CaldavTask
    ): Boolean {
        try {
            val objectId = caldavTask.obj
                ?: run {
                    Timber.e("null obj for caldavTask.id=${caldavTask.id} task.id=${caldavTask.task}")
                    caldavTask.obj = caldavTask.remoteId?.let { "$it.ics" }
                    caldavTask.obj
                }
            if (objectId?.isNotBlank() == true) {
                val remote = DavResource(
                    httpClient = httpClient,
                    location = httpUrl.newBuilder().addPathSegment(objectId).build(),
                )
                remote.delete(null) {}
            }
        } catch (e: HttpException) {
            if (e.statusCode != 404) {
                Timber.e(e)
                return false
            }
        } catch (e: IOException) {
            Timber.e(e)
            return false
        }
        vtodoCache.delete(calendar, caldavTask)
        caldavDao.delete(caldavTask)
        return true
    }

    private suspend fun pushTask(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        task: Task,
        httpClient: OkHttpClient,
        httpUrl: HttpUrl
    ) {
        val caldavTask = caldavDao.getTask(task.id) ?: return
        Timber.d("pushing caldavTask=$caldavTask task=$task")
        if (task.isDeleted) {
            if (deleteRemoteResource(httpClient, httpUrl, calendar, caldavTask)) {
                taskDeleter.delete(task)
            }
            return
        }
        val data = iCal.toVtodo(account, calendar, caldavTask, task)
        val requestBody = data.toRequestBody(contentType = MIME_ICALENDAR)
        val objPath = caldavTask.obj
            ?: run {
                Timber.e("null obj for caldavTask.id=${caldavTask.id} task.id=${task.id}")
                caldavTask.obj = caldavTask.remoteId?.let { "$it.ics" }
                caldavTask.obj
            }
            ?: throw IllegalStateException("Push failed - missing UUID")

        try {
            val remote = DavResource(
                httpClient = httpClient,
                location = httpUrl.newBuilder().addPathSegment(objPath).build(),
            )
            remote.put(requestBody) {
                if (it.isSuccessful) {
                    fromResponse(it)?.eTag?.takeIf(String::isNotBlank)?.let { etag ->
                        caldavTask.etag = etag
                    }
                    runBlocking {
                        vtodoCache.putVtodo(calendar, caldavTask, String(data))
                    }
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

    suspend fun Response.principals(
        account: CaldavAccount,
        list: CaldavCalendar
    ): List<PrincipalAccess> {
        val access = ArrayList<PrincipalAccess>()
        this[Invite::class.java]
            ?.sharees
            ?.filter { it.href?.let { href -> !isCurrentUser(href) } ?: false }
            ?.map {
                val principal = principalDao.getOrCreatePrincipal(
                    account,
                    it.href!!,
                    it.properties
                        .find { p -> p is DisplayName }
                        ?.let { name -> (name as DisplayName).displayName }
                )
                principalDao.getOrCreateAccess(
                    list,
                    principal,
                    invite = it.response?.toStatus ?: INVITE_UNKNOWN,
                    access = it.access?.access?.toAccess ?: ACCESS_UNKNOWN
                )
            }
            ?.let { access.addAll(it) }
        this[OCInvite::class.java]?.users
            ?.map {
                val principal = principalDao.getOrCreatePrincipal(account, it.href)
                principalDao.getOrCreateAccess(
                    list,
                    principal,
                    it.response.toStatus,
                    it.access.access.toAccess
                )
            }
            ?.let {
                if (!isOwncloudOwner) {
                    this@principals[OCOwnerPrincipal::class.java]?.owner?.let { href ->
                        val principal = principalDao.getOrCreatePrincipal(account, href)
                        access.add(principalDao.getOrCreateAccess(
                            list,
                            principal,
                            INVITE_ACCEPTED,
                            ACCESS_OWNER
                        ))
                    }
                }
                access.addAll(it)
            }
        return access
    }

    companion object {
        init {
            org.tasks.caldav.Task.prodId = ProdId(PROD_ID)
        }

        fun registerFactories() {
            PropertyRegistry.register(
                listOf(
                    ShareAccess.Factory(),
                    Invite.Factory(),
                    OCOwnerPrincipal.Factory(),
                    OCInvite.Factory(),
                    CalendarIcon.Factory,
                )
            )
        }

        val Response.ctag: String?
            get() = this[SyncToken::class.java]?.token ?: this[GetCTag::class.java]?.cTag

        val Response.accessLevel: Int
            get() {
                this[ShareAccess::class.java]?.access?.let {
                    return when (it) {
                        NOT_SHARED, SHARED_OWNER -> ACCESS_OWNER
                        READ_WRITE -> ACCESS_READ_WRITE
                        NO_ACCESS, READ -> ACCESS_READ_ONLY
                        else -> ACCESS_UNKNOWN
                    }
                }
                if (isOwncloudOwner) {
                    return ACCESS_OWNER
                }
                return when (this[CurrentUserPrivilegeSet::class.java]?.mayWriteContent) {
                    false -> ACCESS_READ_ONLY
                    else -> ACCESS_READ_WRITE
                }
        }

        private val Response.isOwncloudOwner: Boolean
            get() = this[OCOwnerPrincipal::class.java]?.owner?.let { isCurrentUser(it) } ?: false

        private fun Response.isCurrentUser(href: String) =
            this[CurrentUserPrincipal::class.java]?.href?.endsWith("$href/") == true

        private val Property.Name.toAccess: Int
            get() = when (this) {
                SHARED_OWNER, OCAccess.SHARED_OWNER -> ACCESS_OWNER
                READ_WRITE, OCAccess.READ_WRITE -> ACCESS_READ_WRITE
                READ, OCAccess.READ -> ACCESS_READ_ONLY
                else -> ACCESS_UNKNOWN
            }

        private val Property.Name.toStatus: Int
            get() = when (this) {
                Sharee.INVITE_ACCEPTED, OCUser.INVITE_ACCEPTED -> INVITE_ACCEPTED
                Sharee.INVITE_NORESPONSE, OCUser.INVITE_NORESPONSE -> INVITE_NO_RESPONSE
                Sharee.INVITE_DECLINED, OCUser.INVITE_DECLINED -> INVITE_DECLINED
                Sharee.INVITE_INVALID, OCUser.INVITE_INVALID -> INVITE_INVALID
                else -> INVITE_UNKNOWN
            }
    }
}
