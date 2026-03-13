package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlReader
import org.xmlpull.v1.XmlPullParser

data class CalendarIcon(
    val icon: String,
): Property {
    companion object Companion {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "x-calendar-icon")
    }

    object Factory: PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlPullParser): CalendarIcon {
            val text = XmlReader(parser).readText()?.takeIf { it.isNotBlank() }
            return CalendarIcon(text ?: "")
        }
    }
}
