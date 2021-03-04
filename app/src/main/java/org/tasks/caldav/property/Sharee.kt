package org.tasks.caldav.property

import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.propertyName
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
                    DavResource.HREF ->
                        XmlUtils.readText(parser)?.let { href = it }
                    ShareAccess.NAME ->
                        PropertyRegistry.create(ShareAccess.NAME, parser)
                            ?.let { access = it as ShareAccess }
                    COMMENT ->
                        XmlUtils.readText(parser)?.let { comment = it }
                    INVITE_ACCEPTED, INVITE_DECLINED, INVITE_NORESPONSE, INVITE_INVALID ->
                        response = name
                    DavResource.PROP ->
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
        val COMMENT = Property.Name(XmlUtils.NS_WEBDAV, "comment")
        val INVITE_ACCEPTED = Property.Name(XmlUtils.NS_WEBDAV, "invite-accepted")
        val INVITE_DECLINED = Property.Name(XmlUtils.NS_WEBDAV, "invite-declined")
        val INVITE_NORESPONSE = Property.Name(XmlUtils.NS_WEBDAV, "invite-noresponse")
        val INVITE_INVALID = Property.Name(XmlUtils.NS_WEBDAV, "invite-invalid")
    }
}