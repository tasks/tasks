package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.entity.CaldavCalendar
import org.tasks.makers.Maker.make

object CaldavCalendarMaker {
    val ID: Property<CaldavCalendar, Long> = newProperty()
    val ACCOUNT: Property<CaldavCalendar, String> = newProperty()
    val NAME: Property<CaldavCalendar, String> = newProperty()
    val UUID: Property<CaldavCalendar, String> = newProperty()

    private val instantiator = Instantiator { lookup ->
        CaldavCalendar(
            id = lookup.valueOf(ID, 0L),
            name = lookup.valueOf(NAME, null as String?),
            account = lookup.valueOf(ACCOUNT, "account"),
            uuid = lookup.valueOf(UUID, "calendar"),
        )
    }

    fun newCaldavCalendar(vararg properties: PropertyValue<in CaldavCalendar?, *>): CaldavCalendar {
        return make(instantiator, *properties)
    }
}