package org.tasks.caldav.property

import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.BuildConfig
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.xmlpull.v1.XmlPullParser

class OCUser(parser: XmlPullParser) {
    lateinit var href: String
        private set
    var commonName: String? = null
        private set
    lateinit var access: OCAccess
        private set
    lateinit var response: Property.Name
        private set

    init {
        val depth = parser.depth
        var eventType = parser.eventType
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                when (val name = parser.propertyName()) {
                    DavResource.HREF ->
                        XmlUtils.readText(parser)?.let { href = it }
                    COMMON_NAME ->
                        XmlUtils.readText(parser)?.let { commonName = it }
                    OCAccess.ACCESS ->
                        access = OCAccess(parser)
                    INVITE_ACCEPTED, INVITE_DECLINED, INVITE_NORESPONSE, INVITE_INVALID ->
                        response = name
                }
            }
            eventType = parser.next()
        }
        if (BuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
    }

    companion object {
        val COMMON_NAME = Property.Name(NS_OWNCLOUD, "common-name")
        val INVITE_ACCEPTED = Property.Name(NS_OWNCLOUD, "invite-accepted")
        val INVITE_DECLINED = Property.Name(NS_OWNCLOUD, "invite-declined")
        val INVITE_NORESPONSE = Property.Name(NS_OWNCLOUD, "invite-noresponse")
        val INVITE_INVALID = Property.Name(NS_OWNCLOUD, "invite-invalid")
    }
}