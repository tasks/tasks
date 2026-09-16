package org.tasks.caldav.property

import at.bitfire.dav4jvm.Property

data class CalendarIcon(
    val icon: String,
): Property {
    companion object Companion {
        val NAME = Property.Name(PropertyUtils.NS_TASKS, "x-calendar-icon")
        val Factory = textDeadPropertyFactory(NAME) { CalendarIcon(it) }
    }
}
