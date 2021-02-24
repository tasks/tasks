package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.TestUtilities.readFile

class GeocoderNominatimTest {
    @Test
    fun pitchGeocode() {
        val place = GeocoderNominatim.jsonToPlace(readFile("nominatim/pitch.json"))!!

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
        val place = GeocoderNominatim.jsonToPlace(readFile("nominatim/house.json"))!!

        assertEquals("1 Løvenbergvegen", place.name)
        assertEquals(11.1658572, place.longitude, 0.0)
        assertEquals(60.2301296, place.latitude, 0.0)
        assertEquals(
                "1, Løvenbergvegen, Mogreina, Ullensaker, Viken, 2054, Norge",
                place.address
        )
    }

    @Test
    fun residentialGeocode() {
        val place = GeocoderNominatim.jsonToPlace(readFile("nominatim/residential.json"))!!

        assertNull(place.name)
        assertEquals(-9.553143, place.longitude, 0.0)
        assertEquals(53.8946414, place.latitude, 0.0)
        assertEquals(
            "Newport East ED, Westport-Belmullet Municipal District, County Mayo, Connacht, Éire / Ireland",
            place.address
        )
    }

    @Test
    fun busStopGeocode() {
        val place = GeocoderNominatim.jsonToPlace(readFile("nominatim/bus_stop.json"))!!

        assertEquals("Blessington Road", place.name)
        assertEquals(-6.4154817, place.longitude, 0.0)
        assertEquals(53.2751611, place.latitude, 0.0)
        assertEquals(
            "Blessington Road, Clondalkin ED, Tallaght, South Dublin, County Dublin, Leinster, D24 EP20, Éire / Ireland",
            place.address
        )
    }
}