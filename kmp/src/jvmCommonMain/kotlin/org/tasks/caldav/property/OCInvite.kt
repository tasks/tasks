package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils.propertyName
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import org.tasks.TasksBuildConfig
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD

data class OCInvite(val users: List<OCUser>): Property {

    companion object {
        @JvmField
        val NAME = Property.Name(NS_OWNCLOUD, "invite")

        val USER = Property.Name(NS_OWNCLOUD, "user")
    }

    class Factory : PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlReader): OCInvite {
            val depth = parser.depth
            var eventType = parser.eventType
            val users = ArrayList<OCUser>()
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    if (parser.propertyName() == USER) {
                        users.add(OCUser(parser))
                    }
                }
                eventType = parser.next()
            }
            if (TasksBuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
            return OCInvite(users)
        }
    }
}
