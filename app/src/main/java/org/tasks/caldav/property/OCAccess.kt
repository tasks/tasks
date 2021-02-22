package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.BuildConfig
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
                when (val name = parser.propertyName()) {
                    SHARED_OWNER, READ_WRITE, NOT_SHARED, READ -> access = name
                }
            }
            eventType = parser.next()
        }
        if (BuildConfig.DEBUG && parser.depth != depth) {
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
