package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.readFile

class NominatimGeocoderTest {
    @Test
    fun pitchGeocode() {
        val place = NominatimGeocoder.jsonToPlace(readFile("nominatim/pitch.json"))!!

        assertEquals("Guaranteed Rate Field", place.name)
        assertEquals(-87.63362064328714, place.longitude, 0.0)
        assertEquals(41.82982845, place.latitude, 0.0)
        assertEquals(
                "Guaranteed Rate Field, West 36th Street, Armour Square, Chicago, Cook County, Illinois, 60616, United States",
                place.address
        )
    }

    @Test
    fun houseGeocode() {
        val place = NominatimGeocoder.jsonToPlace(readFile("nominatim/house.json"))!!

        assertEquals("1 Løvenbergvegen", place.name)
        assertEquals(11.1658572, place.longitude, 0.0)
        assertEquals(60.2301296, place.latitude, 0.0)
        assertEquals(
                "1, Løvenbergvegen, Mogreina, Ullensaker, Viken, 2054, Norge",
                place.address
        )
    }
}