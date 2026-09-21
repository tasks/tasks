package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.DavCalendar.Companion.MIME_ICALENDAR
import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.ktor.exception.DavException
import at.bitfire.dav4jvm.ktor.exception.HttpException
import at.bitfire.dav4jvm.ktor.exception.ServiceUnavailableException
import at.bitfire.dav4jvm.ktor.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.caldav.CalendarColor
import at.bitfire.dav4jvm.property.caldav.CalendarData
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrincipal
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrivilegeSet
import at.bitfire.dav4jvm.property.caldav.GetCTag
import at.bitfire.dav4jvm.property.webdav.DisplayName
import at.bitfire.dav4jvm.property.webdav.GetETag
import at.bitfire.dav4jvm.property.webdav.GetETag.Companion.fromHttpResponse
import at.bitfire.dav4jvm.property.webdav.SyncToken
import org.tasks.service.TaskDeleter
import org.tasks.data.dao.DirtyDao
import io.ktor.client.HttpClient
import io.ktor.http.Headers
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.content.ByteArrayContent
import org.jetbrains.compose.resources.getString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.requires_pro_subscription
import org.tasks.analytics.AnalyticsEvents.INITIAL_SYNC_COMPLETE
import org.tasks.analytics.AnalyticsEvents.PARAM_TASK_COUNT
import org.tasks.analytics.AnalyticsEvents.PARAM_TYPE
import org.tasks.analytics.AnalyticsEvents.SYNC_UNKNOWN_ACCESS
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.metadata.TagMetadataSync
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
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_PURCHASE_TOKEN_IN_USE
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
import org.tasks.http.NetworkException
import co.touchlab.kermit.Logger
import kotlinx.io.IOException

private const val TAG = "CaldavSync"

class CaldavSynchronizer(
    private val caldavDao: CaldavDao,
    private val dirtyDao: DirtyDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
    private val provider: CaldavClientProvider,
    private val iCal: iCalendar,
    private val principalDao: PrincipalDao,
    private val vtodoCache: VtodoCache,
    private val accountDataRepository: TasksAccountDataRepository,
    private val tagMetadataSync: TagMetadataSync,
) {
    suspend fun sync(account: CaldavAccount, hasPro: Boolean) {
        Logger.d(TAG) { "Synchronizing $account" }
        if (!hasPro && !account.isTasksOrg) {
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
        } catch (e: PurchaseTokenInUseException) {
            setError(account, "${ERROR_PURCHASE_TOKEN_IN_USE}${e.existingAccount}")
        } catch (e: IOException) {
            setError(account, e)
        } catch (e: UnauthorizedException) {
            setError(account, e)
        } catch (e: ServiceUnavailableException) {
            setError(account, e)
        } catch (e: NetworkException) {
            setError(account, e)
        } catch (e: HttpException) {
            when(e.statusCode) {
                402, 451, in 500..599 -> {}
                else -> { reporting.reportException(e) }
            }
            setError(account, e)
        } catch (e: Exception) {
            setError(account, e)
            if (!e.isTlsSetupFailure()) {
                reporting.reportException(e)
            }
        }
    }

    private suspend fun synchronize(account: CaldavAccount) = provider.forAccount(account).use { caldavClient ->
        var serverType = account.serverType

        // Check guest status for Tasks.org accounts
        val isGuest = if (caldavClient is TasksClient) {
            try {
                accountDataRepository.fetchAndCache(caldavClient)?.guest ?: false
            } catch (e: Exception) {
                Logger.e(e) { "Failed to fetch account data" }
                accountDataRepository.getAccountResponse()?.guest ?: false
            }
        } else false

        val resources = caldavClient.calendars { headers ->
            if (serverType == SERVER_UNKNOWN) {
                serverType = getServerType(account, headers)
            }
        }
        if (serverType != account.serverType) {
            account.serverType = serverType
            caldavDao.update(account)
        }
        val urls = resources.map { it.href.toString() }.toHashSet()
        for (calendar in caldavDao.findDeletedCalendars(account.uuid!!, ArrayList(urls))) {
            taskDeleter.delete(calendar)
        }
        val metadataPulled = if (tagMetadataSync.isPrimary(account)) {
            tagMetadataSync.pullMetadata(account, caldavClient)
        } else {
            null
        }
        for (resource in resources) {
            val url = resource.href.toString()
            var calendar = caldavDao.getCalendarByUrl(account.uuid!!, url)
            val remoteName = resource[DisplayName::class]!!.displayName
            val color = resource[CalendarColor::class]?.color ?: 0
            val rawAccess = resource.accessLevel
            val guestOwned = isGuest && rawAccess == ACCESS_OWNER
            val access = if (guestOwned) ACCESS_READ_ONLY else rawAccess
            val icon = resource[CalendarIcon::class]?.icon?.takeIf { it.isNotBlank() }

            if (rawAccess == ACCESS_UNKNOWN) {
                reporting.logEvent(
                    SYNC_UNKNOWN_ACCESS,
                    PARAM_TYPE to
                            (resource[ShareAccess::class]?.access?.toString() ?: "???")
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
        if (metadataPulled != null &&
            tagMetadataSync.pushAndReap(account, caldavClient, metadataPulled)
        ) {
            refreshBroadcaster.broadcastRefresh()
        }
    }

    private fun getServerType(account: CaldavAccount, headers: Headers): Int {
        val dav = headers.getAll("DAV")?.joinToString(",")
        return when {
            account.isTasksOrg -> SERVER_TASKS
            dav?.contains("oc-resource-sharing") == true ->
                if (dav.contains("nextcloud-") || dav.contains("nc-"))
                    SERVER_NEXTCLOUD
                else
                    SERVER_OWNCLOUD
            headers["x-sabre-version"]?.isNotBlank() == true -> SERVER_SABREDAV
            headers["server"] == "Openexchange WebDAV" -> SERVER_OPEN_XCHANGE
            else -> SERVER_UNKNOWN
        }
    }

    private suspend fun setError(account: CaldavAccount, throwable: Throwable) {
        Logger.e(throwable) { "$account: ${throwable.message}" }
        setError(account, throwable.message)
    }

    private suspend fun setError(account: CaldavAccount, message: String?) {
        if (!message.isNullOrBlank()) {
            Logger.e(TAG) { "$account: $message" }
        }
        account.error = message
        caldavDao.update(account)
        refreshBroadcaster.broadcastRefresh()
        if (!message.isNullOrBlank()) {
            Logger.e(TAG) { message.orEmpty() }
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        resource: Response,
        httpClient: HttpClient
    ) {
        val httpUrl = resource.href
        val remoteCtag = resource.ctag
        if (caldavCalendar.ctag?.equals(remoteCtag) == true) {
            Logger.d(TAG) { "up to date: $caldavCalendar" }
            return
        }
        Logger.d(TAG) { "updating $caldavCalendar" }
        val davCalendar = DavCalendar(httpClient, httpUrl)
        val members = davCalendar.calendarQuery("VTODO", null, null).members()
        val changed = members.filter { vCard: Response ->
            val eTag = vCard[GetETag::class]?.eTag
            if (eTag.isNullOrBlank()) {
                return@filter false
            }
            eTag != caldavDao.getTask(caldavCalendar.uuid!!, vCard.hrefName())?.etag
        }
        for (items in changed.chunked(30)) {
            val urls = items.map { it.href }
            val responses = davCalendar.multiget(urls).members()
            Logger.d(TAG) { "MULTI $urls" }
            for (vCard in responses) {
                val eTag = vCard[GetETag::class]?.eTag
                val url = vCard.href
                if (eTag.isNullOrBlank()) {
                    throw DavException("Received CalDAV GET response without ETag for $url")
                }
                val vtodo = vCard[CalendarData::class]?.iCalendar
                if (vtodo.isNullOrBlank()) {
                    throw DavException("Received CalDAV GET response without CalendarData for $url")
                }
                val fileName = vCard.hrefName()
                val remote = fromVtodo(vtodo)
                if (remote == null) {
                    Logger.e(TAG) { "Invalid VCALENDAR: $fileName" }
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
                    Logger.d(TAG) { "DELETED $it" }
                    val tasks = caldavDao.getTasks(caldavCalendar.uuid!!, it.toList())
                    taskDeleter.delete(tasks.map { it.task })
                }
        caldavCalendar.ctag = remoteCtag
        Logger.d(TAG) { "UPDATE $caldavCalendar" }
        caldavDao.update(caldavCalendar)
        Logger.d(TAG) { "Updating parents for ${caldavCalendar.uuid}" }
        caldavDao.updateParents(caldavCalendar.uuid!!)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        httpClient: HttpClient,
        httpUrl: Url,
        deleteOnly: Boolean = false,
    ) {
        for (task in caldavDao.getMoved(caldavCalendar.uuid!!)) {
            deleteRemoteResource(httpClient, httpUrl, caldavCalendar, task)
        }
        for (toPush in dirtyDao.getTasksToPush(caldavCalendar.uuid!!)) {
            if (deleteOnly && !toPush.task.isDeleted) continue
            pushTask(account, caldavCalendar, toPush.task, toPush.caldavTaskId, toPush.dirtyVersion, httpClient, httpUrl)
        }
    }

    private suspend fun deleteRemoteResource(
        httpClient: HttpClient,
        httpUrl: Url,
        calendar: CaldavCalendar,
        caldavTask: CaldavTask
    ): Boolean {
        try {
            val objectId = caldavTask.obj
                ?: run {
                    Logger.e(TAG) { "null obj for caldavTask.id=${caldavTask.id} task.id=${caldavTask.task}" }
                    caldavTask.obj = caldavTask.remoteId?.let { CaldavTask.objectName(it) }
                    caldavTask.obj
                }
            if (objectId?.isNotBlank() == true) {
                val remote = DavResource(
                    httpClient = httpClient,
                    location = httpUrl.child(objectId),
                )
                remote.delete {}
            }
        } catch (e: HttpException) {
            if (e.statusCode != 404) {
                Logger.e(e) { e.message.orEmpty() }
                return false
            }
        } catch (e: IOException) {
            Logger.e(e) { e.message.orEmpty() }
            return false
        }
        caldavDao.delete(caldavTask)
        return true
    }

    private suspend fun pushTask(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        task: Task,
        caldavTaskId: Long,
        dirtyVersion: Long?,
        httpClient: HttpClient,
        httpUrl: Url
    ) {
        val caldavTask = caldavDao.getCaldavTaskById(caldavTaskId) ?: return
        Logger.d(TAG) { "pushing caldavTask=$caldavTask task=$task" }
        if (task.isDeleted) {
            if (deleteRemoteResource(httpClient, httpUrl, calendar, caldavTask)) {
                taskDeleter.delete(task)
            }
            return
        }
        dirtyDao.withDirtyVersion(caldavTaskId, dirtyVersion) {
            val data = iCal.toVtodo(account, calendar, caldavTask, task)
            val requestBody = ByteArrayContent(data, MIME_ICALENDAR)
            val objPath = caldavTask.obj
                ?: run {
                    Logger.e(TAG) { "null obj for caldavTask.id=${caldavTask.id} task.id=${task.id}" }
                    caldavTask.obj = caldavTask.remoteId?.let { CaldavTask.objectName(it) }
                    caldavTask.obj
                }
                ?: throw IllegalStateException("Push failed - missing UUID")

            try {
                val remote = DavResource(
                    httpClient = httpClient,
                    location = httpUrl.child(objPath),
                )
                remote.put(requestBody) {
                    fromHttpResponse(it)?.eTag?.takeIf(String::isNotBlank)?.let { etag ->
                        caldavTask.etag = etag
                    }
                    vtodoCache.putVtodo(calendar, caldavTask, data.decodeToString())
                }
            } catch (e: HttpException) {
                Logger.e(e) { e.message.orEmpty() }
                throw e
            }
            caldavDao.update(caldavTask)
        }
        Logger.d(TAG) { "SENT $caldavTask" }
    }

    suspend fun Response.principals(
        account: CaldavAccount,
        list: CaldavCalendar
    ): List<PrincipalAccess> {
        val access = ArrayList<PrincipalAccess>()
        this[Invite::class]
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
        this[OCInvite::class]?.users
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
                    this@principals[OCOwnerPrincipal::class]?.owner?.let { href ->
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
        val Response.ctag: String?
            get() = this[SyncToken::class]?.token ?: this[GetCTag::class]?.cTag

        private fun Url.child(segment: String): Url =
            URLBuilder(this).appendPathSegments(segment, encodeSlash = true).build()

        val Response.accessLevel: Int
            get() {
                this[ShareAccess::class]?.access?.let {
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
                return when (this[CurrentUserPrivilegeSet::class]?.mayWriteContent) {
                    false -> ACCESS_READ_ONLY
                    else -> ACCESS_READ_WRITE
                }
        }

        private val Response.isOwncloudOwner: Boolean
            get() = this[OCOwnerPrincipal::class]?.owner?.let { isCurrentUser(it) } ?: false

        private fun Response.isCurrentUser(href: String) =
            this[CurrentUserPrincipal::class]?.href?.endsWith("$href/") == true

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
