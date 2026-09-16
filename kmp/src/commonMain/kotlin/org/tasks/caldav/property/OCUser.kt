package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.property.webdav.WebDAV
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import org.tasks.TasksBuildConfig
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD

class OCUser(parser: XmlReader) {
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
        while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
            if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                when (val name = parser.propertyName()) {
                    WebDAV.Href ->
                        parser.readText()?.let { href = it }
                    COMMON_NAME ->
                        parser.readText()?.let { commonName = it }
                    OCAccess.ACCESS ->
                        access = OCAccess(parser)
                    INVITE_ACCEPTED, INVITE_DECLINED, INVITE_NORESPONSE, INVITE_INVALID ->
                        response = name
                }
            }
            eventType = parser.next()
        }
        if (TasksBuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
    }

    companion object {
        val COMMON_NAME = Property.Name(NS_OWNCLOUD, "common-name")
        val INVITE_ACCEPTED = Property.Name(NS_OWNCLOUD, "invite-accepted")
        val INVITE_DECLINED = Property.Name(NS_OWNCLOUD, "invite-declined")
        val INVITE_NORESPONSE = Property.Name(NS_OWNCLOUD, "invite-noresponse")
        val INVITE_INVALID = Property.Name(NS_OWNCLOUD, "invite-invalid")
    }
}