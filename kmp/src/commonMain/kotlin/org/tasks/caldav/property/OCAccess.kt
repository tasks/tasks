package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.XmlUtils.propertyName
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import org.tasks.TasksBuildConfig
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD

class OCAccess(parser: XmlReader) : Property {

    lateinit var access: Property.Name
        private set

    init {
        val depth = parser.depth
        var eventType = parser.eventType
        while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
            if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                access = parser.propertyName()
            }
            eventType = parser.next()
        }
        if (TasksBuildConfig.DEBUG && parser.depth != depth) {
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
