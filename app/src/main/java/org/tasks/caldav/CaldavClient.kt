package org.tasks.caldav

import androidx.annotation.WorkerThread
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.Response.HrefRelation
import at.bitfire.dav4jvm.XmlUtils.NS_APPLE_ICAL
import at.bitfire.dav4jvm.XmlUtils.NS_CALDAV
import at.bitfire.dav4jvm.XmlUtils.NS_WEBDAV
import at.bitfire.dav4jvm.exception.DavException
import at.bitfire.dav4jvm.exception.HttpException
import at.bitfire.dav4jvm.property.*
import at.bitfire.dav4jvm.property.ResourceType.Companion.CALENDAR
import com.todoroo.astrid.helper.UUIDHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.property.ShareAccess
import org.tasks.data.CaldavAccount
import org.tasks.ui.DisplayableException
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import timber.log.Timber
import java.io.IOException
import java.io.StringWriter
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*

open class CaldavClient(
        private val provider: CaldavClientProvider,
        private val customCertManager: CustomCertManager,
        val httpClient: OkHttpClient,
        private val httpUrl: HttpUrl?
) {
    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun forAccount(account: CaldavAccount) =
            provider.forAccount(account)

    @WorkerThread
    @Throws(DavException::class, IOException::class)
    private fun tryFindPrincipal(link: String): String? {
        val url = httpUrl!!.resolve(link)
        Timber.d("Checking for principal: %s", url)
        val davResource = DavResource(httpClient, url!!)
        val responses = ArrayList<Response>()
        davResource.propfind(0, CurrentUserPrincipal.NAME) { response, _ ->
            responses.add(response)
        }
        if (responses.isNotEmpty()) {
            val response = responses[0]
            val currentUserPrincipal = response[CurrentUserPrincipal::class.java]
            if (currentUserPrincipal != null) {
                val href = currentUserPrincipal.href
                if (!isNullOrEmpty(href)) {
                    return href
                }
            }
        }
        return null
    }

    @WorkerThread
    @Throws(DavException::class, IOException::class)
    private fun findHomeset(): String {
        val davResource = DavResource(httpClient, httpUrl!!)
        val responses = ArrayList<Response>()
        davResource.propfind(0, CalendarHomeSet.NAME) { response, _ ->
            responses.add(response)
        }
        val response = responses[0]
        val calendarHomeSet = response[CalendarHomeSet::class.java]
                ?: throw DisplayableException(R.string.caldav_home_set_not_found)
        val hrefs: List<String> = calendarHomeSet.hrefs
        if (hrefs.size != 1) {
            throw DisplayableException(R.string.caldav_home_set_not_found)
        }
        val homeSet = hrefs[0]
        if (isNullOrEmpty(homeSet)) {
            throw DisplayableException(R.string.caldav_home_set_not_found)
        }
        return davResource.location.resolve(homeSet).toString()
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

    @Throws(IOException::class, DavException::class)
    suspend fun calendars(): List<Response> = withContext(Dispatchers.IO) {
        val davResource = DavResource(httpClient, httpUrl!!)
        val responses = ArrayList<Response>()
        davResource.propfind(
                1,
                ResourceType.NAME,
                DisplayName.NAME,
                SupportedCalendarComponentSet.NAME,
                GetCTag.NAME,
                CalendarColor.NAME,
                SyncToken.NAME,
                ShareAccess.NAME,
        ) { response: Response, relation: HrefRelation ->
            if (relation == HrefRelation.MEMBER) {
                responses.add(response)
            }
        }
        val urls: MutableList<Response> = ArrayList()
        for (member in responses) {
            val resourceType = member[ResourceType::class.java]
            if (resourceType == null
                    || !resourceType.types.contains(CALENDAR)) {
                Timber.d("%s is not a calendar", member)
                continue
            }
            val supportedCalendarComponentSet = member.get(SupportedCalendarComponentSet::class.java)
            if (supportedCalendarComponentSet == null
                    || !supportedCalendarComponentSet.supportsTasks) {
                Timber.d("%s does not support tasks", member)
                continue
            }
            urls.add(member)
        }
        urls
    }

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
    suspend fun updateCollection(displayName: String, color: Int): String = withContext(Dispatchers.IO) {
        val davResource = DavResource(httpClient, httpUrl!!)
        davResource.proppatch(getPropPatchString(displayName, color)) { _, _ -> }
        davResource.location.toString()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun getPropPatchString(displayName: String, color: Int): String {
        val xmlPullParserFactory = XmlPullParserFactory.newInstance()
        val xml = xmlPullParserFactory.newSerializer()
        val stringWriter = StringWriter()
        with(xml) {
            setOutput(stringWriter)
            startDocument("UTF-8", null)
            setPrefix("", NS_WEBDAV)
            setPrefix("CAL", NS_CALDAV)
            startTag(NS_WEBDAV, "propertyupdate")
            startTag(NS_WEBDAV, "set")
            startTag(NS_WEBDAV, "prop")
            setDisplayName(this, displayName)
            if (color != 0) {
                setColor(xml, color)
            }
            endTag(NS_WEBDAV, "prop")
            endTag(NS_WEBDAV, "set")
            if (color == 0) {
                startTag(NS_WEBDAV, "remove")
                startTag(NS_WEBDAV, "prop")
                startTag(NS_APPLE_ICAL, "calendar-color")
                endTag(NS_APPLE_ICAL, "calendar-color")
                endTag(NS_WEBDAV, "prop")
                endTag(NS_WEBDAV, "remove")
            }
            endTag(NS_WEBDAV, "propertyupdate")
            endDocument()
            flush()
        }
        return stringWriter.toString()
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

    fun setForeground(): CaldavClient {
        customCertManager.appInForeground = true
        return this
    }
}