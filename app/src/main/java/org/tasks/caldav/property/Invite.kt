package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.BuildConfig
import org.xmlpull.v1.XmlPullParser

data class Invite(val sharees: List<Sharee>): Property {

    companion object {
        @JvmField
        val NAME = Property.Name(XmlUtils.NS_WEBDAV, "invite")

        val SHAREE = Property.Name(XmlUtils.NS_WEBDAV, "sharee")
    }


    class Factory : PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlPullParser): Invite {
            val depth = parser.depth
            var eventType = parser.eventType
            val sharees = ArrayList<Sharee>()
            while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
                if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                    if (parser.propertyName() == SHAREE) {
                        sharees.add(Sharee(parser))
                    }
                }
                eventType = parser.next()
            }
            if (BuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
            return Invite(sharees)
        }
    }
}
