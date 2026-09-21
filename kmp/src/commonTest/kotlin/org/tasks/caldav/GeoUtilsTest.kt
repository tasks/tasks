package org.tasks.caldav

import org.tasks.caldav.GeoUtils.equalish
import org.tasks.caldav.GeoUtils.latitudeLike
import org.tasks.caldav.GeoUtils.longitudeLike
import org.tasks.icalendar.Geo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoUtilsTest {
    @Test
    fun getLatitudeLike() = assertEquals("42.4347%", Geo(42.434722, -83.985).latitudeLike())

    @Test
    fun getLatitudeLikeShort() = assertEquals("-37.3", Geo(-37.3, -12.68).latitudeLike())

    @Test
    fun getLongitudeLike() = assertEquals("-122.3736%", Geo(45.43, -122.373611).longitudeLike())

    @Test
    fun getLongitudeLikeShort() = assertEquals("-12.68", Geo(-37.3, -12.68).longitudeLike())

    @Test
    fun getWholeNumberLikeMatchesTheStoredReal() = assertEquals("1.0", Geo(1.0, -12.68).latitudeLike())

    @Test
    fun tinyValuesAreWrittenOutInFull() = assertEquals("0.0000%", Geo(0.00001, -12.68).latitudeLike())

    @Test
    fun compareGeo() = assertTrue(Geo(-37.3, -12.68).equalish(Geo(-37.3, -12.68)))

    @Test
    fun compareGeoWithLatTruncation() = assertTrue(Geo(42.434722, -83.985).equalish(Geo(42.4347, -83.985)))

    @Test
    fun compareGeoWithLongTruncation() = assertTrue(Geo(45.43, -122.373611).equalish(Geo(45.43, -122.3736)))

    @Test
    fun compareGeoRightSideNull() = assertFalse(Geo(63.4444, 10.9227).equalish(null))
}
