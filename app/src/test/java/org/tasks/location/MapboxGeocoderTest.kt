package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.readFile
import org.tasks.location.MapboxGeocoder.Companion.jsonToPlace

class MapboxGeocoderTest {
    @Test
    fun poiGeocode() {
        val place = jsonToPlace(readFile("mapbox/poi.json"))!!

        assertEquals("Guaranteed Rate Field", place.name)
        assertEquals(-87.63380599999999, place.longitude, 0.0)
        assertEquals(41.8299365, place.latitude, 0.0)
        assertEquals(
                "Guaranteed Rate Field, 333 W 35th St, Chicago, Illinois 60616, United States",
                place.address
        )
    }

    @Test
    fun addressGeocode() {
        val place = jsonToPlace(readFile("mapbox/address.json"))!!

        assertEquals("120 East 13th Street", place.name)
        assertEquals(40.7330031, place.latitude, 0.0)
        assertEquals(-73.9888929, place.longitude, 0.0)
        assertEquals(
                "120 East 13th Street, New York, New York 10003, United States",
                place.address
        )
    }
}