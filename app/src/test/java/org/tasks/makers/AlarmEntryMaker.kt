package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.jobs.AlarmEntry
import org.tasks.makers.Maker.make
import org.tasks.time.DateTime

object AlarmEntryMaker {
    val ID: Property<AlarmEntry, Long> = newProperty()
    val TASK: Property<AlarmEntry, Long> = newProperty()
    val TIME: Property<AlarmEntry, DateTime> = newProperty()
    val TYPE: Property<AlarmEntry, Int> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<AlarmEntry> ->
        AlarmEntry(
            lookup.valueOf(ID, 0L),
            lookup.valueOf(TASK, 0L),
            lookup.valueOf(TIME, newDateTime()).millis,
            lookup.valueOf(TYPE, TYPE_DATE_TIME)
        )
    }

    fun newAlarmEntry(vararg properties: PropertyValue<in AlarmEntry?, *>): AlarmEntry {
        return make(instantiator, *properties)
    }
}