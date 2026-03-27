package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

data class CalendarIcon(
    val icon: String,
): Property {
    companion object Companion {
        @JvmField
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "x-calendar-icon")
    }

    object Factory: PropertyFactory {

        override fun getName() = NAME

        override fun create(parser: XmlPullParser): CalendarIcon? {
            XmlUtils.readText(parser)?.takeIf { it.isNotBlank() }?.let {
                try {
                    return CalendarIcon(it)
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Couldn't parse icon: $it")
                }
            }
            return null
        }
    }
}
