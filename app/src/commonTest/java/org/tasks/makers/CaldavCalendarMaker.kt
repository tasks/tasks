package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.CaldavCalendar
import org.tasks.makers.Maker.make

object CaldavCalendarMaker {
    val ACCOUNT: Property<CaldavCalendar, String> = newProperty()
    val UUID: Property<CaldavCalendar, String> = newProperty()

    private val instantiator = Instantiator<CaldavCalendar> { lookup ->
        val calendar = CaldavCalendar()
        calendar.account = lookup.valueOf(ACCOUNT, "account")
        calendar.uuid = lookup.valueOf(UUID, "uuid")
        calendar
    }

    fun newCaldavCalendar(vararg properties: PropertyValue<in CaldavCalendar?, *>): CaldavCalendar {
        return make(instantiator, *properties)
    }
}