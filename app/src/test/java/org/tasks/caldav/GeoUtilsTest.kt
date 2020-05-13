package org.tasks.caldav

import net.fortuna.ical4j.model.property.Geo
import org.junit.Assert.*
import org.junit.Test
import org.tasks.caldav.GeoUtils.equalish
import org.tasks.caldav.GeoUtils.latitudeLike
import org.tasks.caldav.GeoUtils.longitudeLike

class GeoUtilsTest {
    @Test
    fun getLatitudeLike() =
        assertEquals("42.4347%", newGeo(42.434722, -83.985).latitudeLike())

    @Test
    fun getLatitudeLikeShort() =
            assertEquals("-37.3", newGeo(-37.3, -12.68).latitudeLike())

    @Test
    fun getLongitudeLike() =
            assertEquals("-122.3736%", newGeo(45.43, -122.373611).longitudeLike())

    @Test
    fun getLongitudeLikeShort() =
            assertEquals("-12.68", newGeo(-37.3, -12.68).longitudeLike())

    @Test
    fun compareGeo() =
            assertTrue(newGeo(-37.3, -12.68).equalish(newGeo(-37.3, -12.68)))

    @Test
    fun compareGeoWithLatTruncation() =
            assertTrue(newGeo(42.434722, -83.985).equalish(newGeo(42.4347, -83.985)))

    @Test
    fun compareGeoWithLongTruncation() =
            assertTrue(newGeo(45.43, -122.373611).equalish(newGeo(45.43, -122.3736)))

    @Test
    fun compareGeoRightSideNull() = assertFalse(newGeo(63.4444, 10.9227).equalish(null))

    private fun newGeo(latitude: Double, longitude: Double) = Geo("${latitude};${longitude}")
}