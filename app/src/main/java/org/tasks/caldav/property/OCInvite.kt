package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils.propertyName
import org.tasks.BuildConfig
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.xmlpull.v1.XmlPullParser

data class OCInvite(val users: List<OCUser>): Property {

    companion object {
        @JvmField
        val NAME = Property.Name(NS_OWNCLOUD, "invite")

        val USER = Property.Name(NS_OWNCLOUD, "user")
    }

    class Factory : PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlPullParser): OCInvite {
            val depth = parser.depth
            var eventType = parser.eventType
            val users = ArrayList<OCUser>()
            while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
                if (eventType == XmlPullParser.START_TAG && parser.depth == depth + 1) {
                    if (parser.propertyName() == USER) {
                        users.add(OCUser(parser))
                    }
                }
                eventType = parser.next()
            }
            if (BuildConfig.DEBUG && parser.depth != depth) { error("Assertion failed") }
            return OCInvite(users)
        }
    }
}
