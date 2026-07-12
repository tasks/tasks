package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.okhttp.DavCollection
import at.bitfire.dav4jvm.okhttp.DavResource
import at.bitfire.dav4jvm.okhttp.DavResource.Companion.MIME_XML
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.okhttp.Response.HrefRelation
import at.bitfire.dav4jvm.okhttp.exception.DavException
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import at.bitfire.dav4jvm.property.caldav.CalDAV
import at.bitfire.dav4jvm.property.caldav.CalDAV.NS_APPLE_ICAL
import at.bitfire.dav4jvm.property.caldav.CalDAV.NS_CALDAV
import at.bitfire.dav4jvm.property.caldav.CalendarColor
import at.bitfire.dav4jvm.property.caldav.CalendarHomeSet
import at.bitfire.dav4jvm.property.caldav.GetCTag
import at.bitfire.dav4jvm.property.caldav.SupportedCalendarComponentSet
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrincipal
import at.bitfire.dav4jvm.property.webdav.CurrentUserPrivilegeSet
import at.bitfire.dav4jvm.property.webdav.DisplayName
import at.bitfire.dav4jvm.property.webdav.ResourceType
import at.bitfire.dav4jvm.property.webdav.SyncToken
import at.bitfire.dav4jvm.property.webdav.WebDAV
import at.bitfire.dav4jvm.property.webdav.WebDAV.NS_WEBDAV
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.caldav_home_set_not_found
import org.tasks.caldav.property.CalendarIcon
import org.tasks.caldav.property.Invite
import org.tasks.caldav.property.MetadataProbe
import org.tasks.caldav.property.MetadataProbeVersion
import org.tasks.caldav.property.OCInvite
import org.tasks.caldav.property.OCOwnerPrincipal
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.tasks.caldav.property.ShareAccess
import org.tasks.caldav.property.TagMetadata
import org.tasks.caldav.property.TagMetadataVersion
import org.tasks.data.UUIDHelper
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
            httpUrl?.resolve(link)?.let { currentUserPrincipalHref(it) }

    private suspend fun <T : Property> propfindProperty(
        url: HttpUrl,
        name: Property.Name,
        type: Class<T>,
    ): T? = withContext(Dispatchers.IO) {
        DavResource(httpClient, url)
            .propfind(0, name)
            .firstOrNull()
            ?.let { (response, _) -> response[type] }
    }

    private suspend fun currentUserPrincipalHref(url: HttpUrl): String? =
            propfindProperty(url, WebDAV.CurrentUserPrincipal, CurrentUserPrincipal::class.java)
                    ?.href
                    ?.takeIf { it.isNotBlank() }

    suspend fun principal(): HttpUrl? = withContext(Dispatchers.IO) {
        currentUserPrincipalHref(httpUrl!!)?.let { httpUrl!!.resolve(it) }
    }

    private suspend fun findHomeset(): String {
        val davResource = DavResource(httpClient, httpUrl!!)
        return davResource
                .propfind(0, CalDAV.CalendarHomeSet)
                .firstOrNull()
                ?.let { (response, _) -> response[CalendarHomeSet::class.java] }
                ?.hrefs?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { davResource.location.resolve(it).toString() }
                ?: throw DisplayableException(Res.string.caldav_home_set_not_found)
    }

    @Throws(IOException::class, DavException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun homeSet(
            username: String? = null,
            password: String? = null
    ): String = withContext(Dispatchers.IO) {
        var unauthorized: HttpException? = null

        suspend fun principalOrNull(link: String): String? =
                try {
                    tryFindPrincipal(link)
                } catch (e: Exception) {
                    if (e is HttpException && e.statusCode == 401) {
                        unauthorized = e
                    }
                    Logger.w(e, tag = "CaldavClient") { "" }
                    null
                }

        val principal = principalOrNull("") ?: principalOrNull("/.well-known/caldav")

        val resolved = if (principal.isNullOrBlank()) httpUrl else httpUrl!!.resolve(principal!!)
        try {
            provider.forUrl(resolved.toString(), username, password).findHomeset()
        } catch (e: Exception) {
            val seen401 = unauthorized
            if (seen401 != null && !(e is HttpException && e.statusCode == 401)) {
                throw seen401
            }
            throw e
        }
    }

    suspend fun calendars(interceptor: (okhttp3.Response) -> okhttp3.Response = { it }): List<Response> =
        DavResource(
            httpClient
                .newBuilder()
                .addNetworkInterceptor { interceptor(it.proceed(it.request())) }
                .build(),
            httpUrl!!
        )
            .propfind(1, *calendarProperties)
            .filter { (response, relation) ->
                relation == HrefRelation.MEMBER &&
                        response[ResourceType::class.java]?.types?.contains(CalDAV.Calendar) == true &&
                        response[SupportedCalendarComponentSet::class.java]?.supportsTasks == true
            }
            .map { (response, _) -> response }

    @Throws(IOException::class, HttpException::class)
    suspend fun tagMetadata(url: HttpUrl): String? =
        propfindProperty(url, TagMetadata.NAME, TagMetadata::class.java)?.json?.takeIf { it.isNotBlank() }

    @Throws(IOException::class, HttpException::class)
    suspend fun tagMetadataVersion(url: HttpUrl): String? =
        propfindProperty(url, TagMetadataVersion.NAME, TagMetadataVersion::class.java)
            ?.version
            ?.takeIf { it.isNotBlank() }

    @Throws(IOException::class)
    suspend fun pushTagMetadata(url: HttpUrl, json: String, version: String): Boolean =
        pushProperty(url, TagMetadata.NAME, TagMetadataVersion.NAME, json, version)

    @Throws(IOException::class)
    suspend fun pushMetadataProbe(url: HttpUrl, json: String, version: String): Boolean =
        pushProperty(url, MetadataProbe.NAME, MetadataProbeVersion.NAME, json, version)

    @Throws(IOException::class, HttpException::class)
    suspend fun metadataProbeWithVersion(url: HttpUrl): Pair<String?, String?> = withContext(Dispatchers.IO) {
        DavResource(httpClient, url)
            .propfind(0, MetadataProbe.NAME, MetadataProbeVersion.NAME)
            .firstOrNull()
            ?.let { (response, _) ->
                val payload = response[MetadataProbe::class.java]?.json?.takeIf { it.isNotBlank() }
                val version = response[MetadataProbeVersion::class.java]?.version?.takeIf { it.isNotBlank() }
                payload to version
            }
            ?: (null to null)
    }

    @Throws(IOException::class)
    suspend fun removeMetadataProbe(url: HttpUrl): Boolean =
        proppatch(url, proppatchBody(set = emptyList(), remove = listOf(MetadataProbe.NAME, MetadataProbeVersion.NAME)))

    private suspend fun pushProperty(
        url: HttpUrl,
        property: Property.Name,
        versionProperty: Property.Name,
        json: String,
        version: String,
    ): Boolean = withContext(Dispatchers.IO) {
        proppatch(
            url,
            proppatchBody(
                set = listOf(property to json, versionProperty to version),
                remove = emptyList(),
            )
        )
    }

    private suspend fun proppatch(url: HttpUrl, body: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .method("PROPPATCH", body.toRequestBody(MIME_XML))
            .build()
        httpClient.newCall(request).execute().use { response ->
            when {
                response.code == 403 || response.code == 405 -> {
                    Logger.w(tag = "CaldavClient") { "metadata PROPPATCH refused (${response.code}) at $url" }
                    false
                }
                response.code == 207 -> {
                    when (val failure = propstatFailureCode(response.body?.string())) {
                        null -> true
                        in 500..599 -> throw IOException("metadata PROPPATCH transient failure ($failure) at $url")
                        else -> {
                            Logger.w(tag = "CaldavClient") { "metadata propstat refused ($failure) at $url" }
                            false
                        }
                    }
                }
                response.isSuccessful -> true
                else -> throw IOException("metadata PROPPATCH failed: HTTP ${response.code} at $url")
            }
        }
    }

    @Throws(IOException::class, HttpException::class)
    suspend fun deleteCollection() = withContext(Dispatchers.IO) {
        DavResource(httpClient, httpUrl!!).delete(null) {}
    }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun makeCollection(displayName: String, color: Int, icon: String?): String = withContext(Dispatchers.IO) {
        val davResource = DavResource(httpClient, httpUrl!!.resolve(UUIDHelper.newUUID() + "/")!!)
        val mkcolString = getMkcolString(displayName, color)
        davResource.mkCol(mkcolString) {}
        if (icon?.isNotBlank() == true) {
            davResource.proppatch(CalendarIcon.NAME, icon)
        }
        davResource.location.toString()
    }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun updateCollection(displayName: String, color: Int, icon: String?): String =
        withContext(Dispatchers.IO) {
            with(DavResource(httpClient, httpUrl!!)) {
                proppatch(WebDAV.DisplayName, displayName)
                if (color != 0) {
                    proppatch(
                        CalDAV.CalendarColor,
                        String.format("#%06X", color and 0xFFFFFF)
                    )
                }
                if (icon?.isNotBlank() == true) {
                    proppatch(CalendarIcon.NAME, icon)
                }
                location.toString()
            }
        }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun updateIcon(url: HttpUrl, icon: String?, onFailure: () -> Unit) =
        withContext(Dispatchers.IO) {
            with(DavResource(httpClient, url)) {
                if (icon?.isNotBlank() == true) {
                    proppatch(CalendarIcon.NAME, icon, onFailure)
                }
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
        xml.text(String.format("#%06X", color and 0xFFFFFF))
        xml.endTag(NS_APPLE_ICAL, "calendar-color")
    }

    suspend fun share(
        account: CaldavAccount,
        href: String,
    ) {
        when (account.serverType) {
            SERVER_TASKS, SERVER_SABREDAV -> shareSabredav(href)
            SERVER_OWNCLOUD, SERVER_NEXTCLOUD -> shareOwncloud(href)
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
            SERVER_TASKS, SERVER_SABREDAV -> removeSabrePrincipal(calendar, href)
            SERVER_OWNCLOUD, SERVER_NEXTCLOUD -> removeOwncloudPrincipal(calendar, href)
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
            WebDAV.ResourceType,
            WebDAV.DisplayName,
            CalDAV.SupportedCalendarComponentSet,
            CalDAV.GetCTag,
            CalDAV.CalendarColor,
            WebDAV.SyncToken,
            ShareAccess.NAME,
            Invite.NAME,
            OCOwnerPrincipal.NAME,
            OCInvite.NAME,
            WebDAV.CurrentUserPrivilegeSet,
            WebDAV.CurrentUserPrincipal,
            CalendarIcon.NAME,
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

        fun DavResource.proppatch(
            property: Property.Name,
            value: String,
            onFailure: () -> Unit = {},
        ) {
            proppatch(
                setProperties = mapOf(property to value),
                removeProperties = emptyList(),
                callback = { response, _ ->
                    if (!response.isSuccess()) {
                        Logger.e(tag = "CaldavClient") { "${response.status} when updating $property: ${response.error}" }
                        onFailure()
                    }
                },
            )
        }
    }
}
