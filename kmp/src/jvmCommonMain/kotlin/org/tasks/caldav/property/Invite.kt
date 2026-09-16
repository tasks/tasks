package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.property.webdav.WebDAV
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import org.tasks.TasksBuildConfig

data class Invite(val sharees: List<Sharee>): Property {

    companion object {
        @JvmField
        val NAME = Property.Name(WebDAV.NS_WEBDAV, "invite")

        val SHAREE = Property.Name(WebDAV.NS_WEBDAV, "sharee")
    }


    class Factory : PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlReader): Invite {
            val depth = parser.depth
            var eventType = parser.eventType
            val sharees = ArrayList<Sharee>()
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    if (parser.propertyName() == SHAREE) {
                        sharees.add(Sharee(parser))
                    }
                }
                eventType = parser.next()
            }
            if (TasksBuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
            return Invite(sharees)
        }
    }
}
