package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.ktor.DavCollection
import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.dav4jvm.ktor.DavResource.Companion.MIME_XML_UTF8
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.ktor.exception.DavException
import at.bitfire.dav4jvm.ktor.exception.HttpException
import at.bitfire.dav4jvm.ktor.resolve
import at.bitfire.dav4jvm.ktor.responses
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
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.HttpResponseReceived
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
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
import org.tasks.http.translateExceptions
import org.tasks.ui.DisplayableException
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.Closeable
import java.io.IOException
import java.io.StringWriter

open class CaldavClient(
        val httpClient: HttpClient,
        private val httpUrl: Url?
) : Closeable, CaldavCollectionClient, CaldavDiscoveryClient {
    override fun close() = httpClient.close()

    private suspend fun tryFindPrincipal(link: String): String? =
            httpUrl?.resolve(link)?.let { currentUserPrincipalHref(it) }

    private suspend fun <T : Property> propfindProperty(
        url: Url,
        name: Property.Name,
        type: Class<T>,
    ): T? = withContext(Dispatchers.IO) {
        DavResource(httpClient, url)
            .propfind(0, name)
            .responses()
            .firstOrNull()
            ?.let { it[type] }
    }

    private suspend fun currentUserPrincipalHref(url: Url): String? =
            propfindProperty(url, WebDAV.CurrentUserPrincipal, CurrentUserPrincipal::class.java)
                    ?.href
                    ?.takeIf { it.isNotBlank() }

    suspend fun principal(): Url? = withContext(Dispatchers.IO) {
        currentUserPrincipalHref(httpUrl!!)?.let { httpUrl!!.resolve(it) }
    }

    private suspend fun findHomeset(url: Url): String {
        val davResource = DavResource(httpClient, url)
        return davResource
                .propfind(0, CalDAV.CalendarHomeSet)
                .responses()
                .firstOrNull()
                ?.let { it[CalendarHomeSet::class.java] }
                ?.hrefs?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { davResource.location.resolve(it)?.canonical()?.toString() }
                ?: throw DisplayableException(Res.string.caldav_home_set_not_found)
    }

    @Throws(IOException::class, DavException::class)
    override suspend fun homeSet(): String = translateExceptions {
        withContext(Dispatchers.IO) {
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

            try {
                findHomeset(principal?.let { httpUrl!!.resolve(it) } ?: httpUrl!!)
            } catch (e: Exception) {
                val seen401 = unauthorized
                if (seen401 != null && !(e is HttpException && e.statusCode == 401)) {
                    throw seen401
                }
                throw e
            }
        }
    }

    suspend fun calendars(onResponse: (Headers) -> Unit = {}): List<Response> {
        val subscription = httpClient.monitor.subscribe(HttpResponseReceived) { onResponse(it.headers) }
        try {
            return DavResource(httpClient, httpUrl!!)
                .propfind(1, *calendarProperties)
                .members()
                .filter { response ->
                    response[ResourceType::class.java]?.types?.contains(CalDAV.Calendar) == true &&
                            response[SupportedCalendarComponentSet::class.java]?.supportsTasks == true
                }
        } finally {
            subscription.dispose()
        }
    }

    @Throws(IOException::class, HttpException::class)
    suspend fun tagMetadata(url: Url): String? =
        propfindProperty(url, TagMetadata.NAME, TagMetadata::class.java)?.json?.takeIf { it.isNotBlank() }

    @Throws(IOException::class, HttpException::class)
    suspend fun tagMetadataVersion(url: Url): String? =
        propfindProperty(url, TagMetadataVersion.NAME, TagMetadataVersion::class.java)
            ?.version
            ?.takeIf { it.isNotBlank() }

    @Throws(IOException::class)
    suspend fun pushTagMetadata(url: Url, json: String, version: String): Boolean =
        pushProperty(url, TagMetadata.NAME, TagMetadataVersion.NAME, json, version)

    @Throws(IOException::class)
    suspend fun pushMetadataProbe(url: Url, json: String, version: String): Boolean =
        pushProperty(url, MetadataProbe.NAME, MetadataProbeVersion.NAME, json, version)

    @Throws(IOException::class, HttpException::class)
    suspend fun metadataProbeWithVersion(url: Url): Pair<String?, String?> = withContext(Dispatchers.IO) {
        DavResource(httpClient, url)
            .propfind(0, MetadataProbe.NAME, MetadataProbeVersion.NAME)
            .responses()
            .firstOrNull()
            ?.let { response ->
                val payload = response[MetadataProbe::class.java]?.json?.takeIf { it.isNotBlank() }
                val version = response[MetadataProbeVersion::class.java]?.version?.takeIf { it.isNotBlank() }
                payload to version
            }
            ?: (null to null)
    }

    @Throws(IOException::class)
    suspend fun removeMetadataProbe(url: Url): Boolean =
        proppatch(url, proppatchBody(set = emptyList(), remove = listOf(MetadataProbe.NAME, MetadataProbeVersion.NAME)))

    private suspend fun pushProperty(
        url: Url,
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

    private suspend fun proppatch(url: Url, body: String): Boolean = withContext(Dispatchers.IO) {
        val response = httpClient.request(url) {
            method = HttpMethod.parse("PROPPATCH")
            contentType(MIME_XML_UTF8)
            setBody(body)
        }
        val code = response.status.value
        when {
            code == 403 || code == 405 -> {
                Logger.w(tag = "CaldavClient") { "metadata PROPPATCH refused ($code) at $url" }
                false
            }
            code == 207 -> {
                when (val failure = propstatFailureCode(response.bodyAsText())) {
                    null -> true
                    in 500..599 -> throw IOException("metadata PROPPATCH transient failure ($failure) at $url")
                    else -> {
                        Logger.w(tag = "CaldavClient") { "metadata propstat refused ($failure) at $url" }
                        false
                    }
                }
            }
            response.status.isSuccess() -> true
            else -> throw IOException("metadata PROPPATCH failed: HTTP $code at $url")
        }
    }

    @Throws(IOException::class)
    override suspend fun deleteCollection() = translateExceptions {
        withContext(Dispatchers.IO) {
            DavResource(httpClient, httpUrl!!).delete {}
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    override suspend fun makeCollection(displayName: String, color: Int, icon: String?): String = translateExceptions {
        withContext(Dispatchers.IO) {
            val davResource = DavResource(httpClient, httpUrl!!.resolve(UUIDHelper.newUUID() + "/")!!)
            val mkcolString = getMkcolString(displayName, color)
            davResource.mkCol(mkcolString) {}
            if (icon?.isNotBlank() == true) {
                davResource.proppatch(CalendarIcon.NAME, icon)
            }
            davResource.location.canonical().toString()
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    override suspend fun updateCollection(displayName: String, color: Int, icon: String?) = translateExceptions {
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
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class, HttpException::class)
    suspend fun updateIcon(url: Url, icon: String?, onFailure: () -> Unit) =
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

    override suspend fun share(
        account: CaldavAccount,
        href: String,
    ) = translateExceptions {
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
                """.trimIndent().toXml()
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
                    """.trimIndent().toSharing()) {}
        }

    override suspend fun removePrincipal(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        href: String,
    ) = translateExceptions {
        when (account.serverType) {
            SERVER_TASKS, SERVER_SABREDAV -> removeSabrePrincipal(calendar, href)
            SERVER_OWNCLOUD, SERVER_NEXTCLOUD -> removeOwncloudPrincipal(calendar, href)
            else -> throw IllegalArgumentException()
        }
    }

    private suspend fun removeOwncloudPrincipal(calendar: CaldavCalendar, href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, calendar.url!!.toCaldavUrl())
                .post(
                    """
                    <x4:share xmlns:x4="$NS_OWNCLOUD">
                        <x4:remove>
                            <x0:href xmlns:x0="$NS_WEBDAV">$href</x0:href>
                        </x4:remove>
                    </x4:share>
                    """.trimIndent().toXml()
                ) {}
        }

    private suspend fun removeSabrePrincipal(calendar: CaldavCalendar, href: String) =
        withContext(Dispatchers.IO) {
            DavCollection(httpClient, calendar.url!!.toCaldavUrl())
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
                    """.trimIndent().toSharing()
                ) {}
        }

    companion object {
        private val MEDIATYPE_SHARING = ContentType.parse("application/davsharing+xml")

        private fun String.toXml() = TextContent(this, MIME_XML_UTF8)

        private fun String.toSharing() = TextContent(this, MEDIATYPE_SHARING)

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

        suspend fun DavResource.proppatch(
            property: Property.Name,
            value: String,
            onFailure: () -> Unit = {},
        ) {
            proppatch(
                setProperties = mapOf(property to value),
                removeProperties = emptyList(),
            )
                .responses()
                .collect { response ->
                    if (!response.isSuccess()) {
                        Logger.e(tag = "CaldavClient") { "${response.status} when updating $property: ${response.error}" }
                        onFailure()
                    }
                }
        }
    }
}
