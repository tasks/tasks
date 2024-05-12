package org.tasks.caldav

import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.DavResource.Companion.MIME_XML
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.Response.HrefRelation
import at.bitfire.dav4jvm.XmlUtils.NS_APPLE_ICAL
import at.bitfire.dav4jvm.XmlUtils.NS_CALDAV
import at.bitfire.dav4jvm.XmlUtils.NS_WEBDAV
import at.bitfire.dav4jvm.exception.DavException
import at.bitfire.dav4jvm.exception.HttpException
import at.bitfire.dav4jvm.property.*
import at.bitfire.dav4jvm.property.ResourceType.Companion.CALENDAR
import org.tasks.data.UUIDHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.property.Invite
import org.tasks.caldav.property.OCInvite
import org.tasks.caldav.property.OCOwnerPrincipal
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.tasks.caldav.property.ShareAccess
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.ui.DisplayableException
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import timber.log.Timber
import java.io.IOException
import java.io.StringWriter
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import kotlin.coroutines.suspendCoroutine

open class CaldavClient(
        private val provider: CaldavClientProvider,
        val httpClient: OkHttpClient,
        private val httpUrl: HttpUrl?
) {
    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun forAccount(account: CaldavAccount) =
            provider.forAccount(account)

    private suspend fun tryFindPrincipal(link: String): String? =
            httpUrl
                    ?.resolve(link)
                    ?.let { DavResource(httpClient, it) }
                    ?.propfind(0, CurrentUserPrincipal.NAME)
                    ?.firstOrNull()
                    ?.let { (response, _) -> response[CurrentUserPrincipal::class.java] }
                    ?.href
                    ?.takeIf { it.isNotBlank() }

    private suspend fun findHomeset(): String {
        val davResource = DavResource(httpClient, httpUrl!!)
        return davResource
                .propfind(0, CalendarHomeSet.NAME)
                .firstOrNull()
                ?.let { (response, _) -> response[CalendarHomeSet::class.java] }
                ?.href
                ?.takeIf { it.isNotBlank() }
                ?.let { davResource.location.resolve(it).toString() }
                ?: throw DisplayableException(R.string.caldav_home_set_not_found)
    }

    @Throws(IOException::class, DavException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun homeSet(
            username: String? = null,
            password: String? = null
    ): String = withContext(Dispatchers.IO) {
        var principal: String? = null
        try {
            principal = tryFindPrincipal("/.well-known/caldav")
        } catch (e: Exception) {
            if (e is HttpException && e.code == 401) {
                throw e
            }
            Timber.w(e)
        }
        if (principal == null) {
            principal = tryFindPrincipal("")
        }
        provider.forUrl(
                (if (isNullOrEmpty(principal)) httpUrl else httpUrl!!.resolve(principal!!)).toString(),
                username,
                password)
                .findHomeset()
    }

    suspend fun calendars(interceptor: (Interceptor.Chain) -> okhttp3.Response): List<Response> =
        DavResource(
            httpClient.newBuilder().addNetworkInterceptor(interceptor).build(),
            httpUrl!!
        )
            .propfind(1, *calendarProperties)
            .filter { (response, relation) ->
                relation == HrefRelation.MEMBER &&
                        response[ResourceType::class.java]?.types?.contains(CALENDAR) == true &&
                        response[SupportedCalendarComponentSet::class.java]?.supportsTasks == true
            }
            .map { (response, _) -> response }

    @Throws(IOException::class, HttpException::class)
    suspend fun deleteCollection() = withContext(Dispatchers.IO) {
        DavResource(httpClient, httpUrl!!).delete(null) {}
    }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun makeCollection(displayName: String, color: Int): String = withContext(Dispatchers.IO) {
        val davResource = DavResource(httpClient, httpUrl!!.resolve(UUIDHelper.newUUID() + "/")!!)
        val mkcolString = getMkcolString(displayName, color)
        davResource.mkCol(mkcolString) {}
        davResource.location.toString()
    }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun updateCollection(displayName: String, color: Int): String =
        withContext(Dispatchers.IO) {
            with(DavResource(httpClient, httpUrl!!)) {
                proppatch(
                    setProperties = mutableMapOf(DisplayName.NAME to displayName).apply {
                        if (color != 0) {
                            put(
                                CalendarColor.NAME,
                                String.format("#%06X%02X", color and 0xFFFFFF, color ushr 24)
                            )
                        }
                    },
                    removeProperties = if (color == 0) listOf(CalendarColor.NAME) else emptyList(),
                    callback = { _, _ -> },
                )
                location.toString()
            }
        }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getMkcolString(displayName: String, color: Int): String {
        val xmlPullParserFactory = XmlPullParserFactory.newInstance()
        val xml = xmlPullParserFactory.newSerializer()
        val stringWriter = StringWriter()
        with(xml) {
            setOutput(stringWriter)
            startDocument("UTF-8", null)
            setPrefix("", NS_WEBDAV)
            setPrefix("CAL", NS_CALDAV)
            startTag(NS_WEBDAV, "mkcol")
            startTag(NS_WEBDAV, "set")
            startTag(NS_WEBDAV, "prop")
            startTag(NS_WEBDAV, "resourcetype")
            startTag(NS_WEBDAV, "collection")
            endTag(NS_WEBDAV, "collection")
            startTag(NS_CALDAV, "calendar")
            endTag(NS_CALDAV, "calendar")
            endTag(NS_WEBDAV, "resourcetype")
            setDisplayName(xml, displayName)
            if (color != 0) {
                setColor(xml, color)
            }
            startTag(NS_CALDAV, "supported-calendar-component-set")
            startTag(NS_CALDAV, "comp")
            attribute(null, "name", "VTODO")
            endTag(NS_CALDAV, "comp")
            endTag(NS_CALDAV, "supported-calendar-component-set")
            endTag(NS_WEBDAV, "prop")
            endTag(NS_WEBDAV, "set")
            endTag(NS_WEBDAV, "mkcol")
            endDocument()
            flush()
        }
        return stringWriter.toString()
    }

    @Throws(IOException::class)
    private fun setDisplayName(xml: XmlSerializer, name: String) {
        xml.startTag(NS_WEBDAV, "displayname")
        xml.text(name)
        xml.endTag(NS_WEBDAV, "displayname")
    }

    @Throws(IOException::class)
    private fun setColor(xml: XmlSerializer, color: Int) {
        xml.startTag(NS_APPLE_ICAL, "calendar-color")
        xml.text(String.format("#%06X%02X", color and 0xFFFFFF, color ushr 24))
        xml.endTag(NS_APPLE_ICAL, "calendar-color")
    }

    suspend fun share(
        account: CaldavAccount,
        href: String,
    ) {
        when (account.serverType) {
            SERVER_TASKS, SERVER_SABREDAV, SERVER_NEXTCLOUD -> shareSabredav(href)
            SERVER_OWNCLOUD -> shareOwncloud(href)
            else -> throw IllegalArgumentException()
        }
    }

    private suspend fun shareOwncloud(href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, httpUrl!!)
                .post("""
                    <x4:share xmlns:x4="$NS_OWNCLOUD">
                        <x4:set>
                            <x0:href xmlns:x0="$NS_WEBDAV">$href</x0:href>
                        </x4:set>
                    </x4:share>
                """.trimIndent().toRequestBody(MIME_XML)
                ) {}
        }

    private suspend fun shareSabredav(href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, httpUrl!!)
                .post("""
                    <D:share-resource xmlns:D="$NS_WEBDAV">
                        <D:sharee>
                            <D:href>$href</D:href>
                            <D:share-access>
                                <D:read-write />
                            </D:share-access>
                        </D:sharee>
                    </D:share-resource>
                    """.trimIndent().toRequestBody(MEDIATYPE_SHARING)) {}
        }

    suspend fun removePrincipal(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        href: String,
    ) {
        when (account.serverType) {
            SERVER_TASKS, SERVER_SABREDAV, SERVER_NEXTCLOUD -> removeSabrePrincipal(calendar, href)
            SERVER_OWNCLOUD -> removeOwncloudPrincipal(calendar, href)
            else -> throw IllegalArgumentException()
        }
    }

    private suspend fun removeOwncloudPrincipal(calendar: CaldavCalendar, href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, calendar.url!!.toHttpUrl())
                .post(
                    """
                    <x4:share xmlns:x4="$NS_OWNCLOUD">
                        <x4:remove>
                            <x0:href xmlns:x0="$NS_WEBDAV">$href</x0:href>
                        </x4:remove>
                    </x4:share>
                    """.trimIndent().toRequestBody(MIME_XML)
                ) {}
        }

    private suspend fun removeSabrePrincipal(calendar: CaldavCalendar, href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, calendar.url!!.toHttpUrl())
                .post(
                    """
                    <D:share-resource xmlns:D="$NS_WEBDAV">
                        <D:sharee>
                            <D:href>$href</D:href>
                            <D:share-access>
                                <D:no-access />
                            </D:share-access>
                        </D:sharee>
                    </D:share-resource>
                    """.trimIndent().toRequestBody(MEDIATYPE_SHARING)
                ) {}
        }

    companion object {
        private val MEDIATYPE_SHARING = "application/davsharing+xml".toMediaType()

        private val calendarProperties = arrayOf(
                ResourceType.NAME,
                DisplayName.NAME,
                SupportedCalendarComponentSet.NAME,
                GetCTag.NAME,
                CalendarColor.NAME,
                SyncToken.NAME,
                ShareAccess.NAME,
                Invite.NAME,
                OCOwnerPrincipal.NAME,
                OCInvite.NAME,
                CurrentUserPrivilegeSet.NAME,
                CurrentUserPrincipal.NAME,
        )

        private suspend fun DavResource.propfind(
                depth: Int,
                vararg reqProp: Property.Name
        ): List<Pair<Response, HrefRelation>> =
                withContext(Dispatchers.IO) {
                    suspendCoroutine { cont ->
                        val responses = ArrayList<Pair<Response, HrefRelation>>()
                        propfind(depth, *reqProp) { response, relation ->
                            responses.add(Pair(response, relation))
                        }
                        cont.resumeWith(Result.success(responses))
                    }
                }
    }
}