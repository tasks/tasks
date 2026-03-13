package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.XmlReader
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.property.webdav.WebDAV
import org.tasks.BuildConfig
import org.xmlpull.v1.XmlPullParser

class Sharee(parser: XmlPullParser) {
    var href: String? = null
        private set
    var access: ShareAccess? = null
        private set
    var response: Property.Name? = null
        private set
    var comment: String? = null
        private set
    val properties = ArrayList<Property>()

    init {
        val depth = parser.depth
        var eventType = parser.eventType
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                when (val name = parser.propertyName()) {
                    WebDAV.Href ->
                        XmlReader(parser).readText()?.let { href = it }
                    ShareAccess.NAME ->
                        PropertyRegistry.create(ShareAccess.NAME, parser)
                            ?.let { access = it as ShareAccess }
                    COMMENT ->
                        XmlReader(parser).readText()?.let { comment = it }
                    INVITE_ACCEPTED, INVITE_DECLINED, INVITE_NORESPONSE, INVITE_INVALID ->
                        response = name
                    WebDAV.Prop ->
                        properties.addAll(Property.parse(parser))
                }
            }
            eventType = parser.next()
        }
        if (BuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
    }

    override fun toString(): String {
        return "Sharee(href='$href', access=$access, response=$response, comment=$comment, properties=$properties)"
    }

    companion object {
        val COMMENT = Property.Name(WebDAV.NS_WEBDAV, "comment")
        val INVITE_ACCEPTED = Property.Name(WebDAV.NS_WEBDAV, "invite-accepted")
        val INVITE_DECLINED = Property.Name(WebDAV.NS_WEBDAV, "invite-declined")
        val INVITE_NORESPONSE = Property.Name(WebDAV.NS_WEBDAV, "invite-noresponse")
        val INVITE_INVALID = Property.Name(WebDAV.NS_WEBDAV, "invite-invalid")
    }
}