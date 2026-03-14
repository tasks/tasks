package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.kmp.IS_DEBUG
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.xmlpull.v1.XmlPullParser

class OCAccess(parser: XmlPullParser) : Property {

    lateinit var access: Property.Name
        private set

    init {
        val depth = parser.depth
        var eventType = parser.eventType
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                access = parser.propertyName()
            }
            eventType = parser.next()
        }
        if (IS_DEBUG && parser.depth != depth) {
            error("Assertion failed")
        }
    }

    override fun toString(): String {
        return "OCAccess(access=$access)"
    }

    companion object {
        val ACCESS = Property.Name(NS_OWNCLOUD, "access")
        val SHARED_OWNER = Property.Name(NS_OWNCLOUD, "shared-owner")
        val READ_WRITE = Property.Name(NS_OWNCLOUD, "read-write")
        val NOT_SHARED = Property.Name(NS_OWNCLOUD, "not-shared")
        val READ = Property.Name(NS_OWNCLOUD, "read")
    }
}
