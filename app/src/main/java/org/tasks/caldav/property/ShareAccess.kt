package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.BuildConfig
import org.xmlpull.v1.XmlPullParser

data class ShareAccess(val access: Property.Name): Property {

    companion object {
        @JvmField
        val NAME = Property.Name(XmlUtils.NS_WEBDAV, "share-access")

        val SHARED_OWNER = Property.Name(XmlUtils.NS_WEBDAV, "shared-owner")
        val READ_WRITE = Property.Name(XmlUtils.NS_WEBDAV, "read-write")
        val NOT_SHARED = Property.Name(XmlUtils.NS_WEBDAV, "not-shared")
        val READ = Property.Name(XmlUtils.NS_WEBDAV, "read")
    }

    override fun toString(): String {
        return "ShareAccess(access=$access)"
    }

    class Factory : PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlPullParser): ShareAccess? {
            // <!ELEMENT share-access #PCDATA>
            var result: Property.Name? = null
            val depth = parser.depth
            var eventType = parser.eventType
            while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
                if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                    when (val name = parser.propertyName()) {
                        SHARED_OWNER, READ_WRITE, NOT_SHARED, READ -> result = name
                    }
                }
                eventType = parser.next()
            }
            if (BuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
            return result?.let { ShareAccess(it) }
        }
    }
}
