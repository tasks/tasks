package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.Geofence

object GeofenceMaker {
    val PLACE: Property<Geofence, String> = Property.newProperty()
    val TASK: Property<Geofence, Long> = Property.newProperty()
    val ARRIVAL: Property<Geofence, Boolean> = Property.newProperty()
    val DEPARTURE: Property<Geofence, Boolean> = Property.newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Geofence> ->
        val geofence = Geofence()
        geofence.place = lookup.valueOf(PLACE, "")
        geofence.task = lookup.valueOf(TASK, 1)
        geofence.isArrival = lookup.valueOf(ARRIVAL, false)
        geofence.isDeparture = lookup.valueOf(DEPARTURE, false)
        geofence
    }

    fun newGeofence(vararg properties: PropertyValue<in Geofence?, *>): Geofence {
        return Maker.make(instantiator, *properties)
    }
}