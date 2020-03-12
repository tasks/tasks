package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.Place

object PlaceMaker {
    val LATITUDE: Property<Place, Double> = Property.newProperty()
    val LONGITUDE: Property<Place, Double> = Property.newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Place> ->
        val place = Place()
        place.latitude = lookup.valueOf(LATITUDE, 0.0)
        place.longitude = lookup.valueOf(LONGITUDE, 0.0)
        place
    }

    fun newPlace(vararg properties: PropertyValue<in Place?, *>): Place {
        return Maker.make(instantiator, *properties)
    }
}