package org.tasks.caldav

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoLikeTest {
    @Test
    fun truncatesBeyondPlaceAccuracy() = assertEquals("42.4347%", 42.434722.toLikeString())

    @Test
    fun keepsShortDecimals() = assertEquals("-37.3", (-37.3).toLikeString())

    @Test
    fun truncatesNegative() = assertEquals("-122.3736%", (-122.373611).toLikeString())

    @Test
    fun exactlyFourPlaces() = assertEquals("12.3456%", 12.3456.toLikeString())

    @Test
    fun wholeNumber() = assertEquals("1.0", 1.0.toLikeString())

    @Test
    fun trailingZeros() = assertEquals("100.0", 100.0.toLikeString())

    @Test
    fun zero() = assertEquals("0.0", 0.0.toLikeString())

    @Test
    fun negativeZero() = assertEquals("0.0", (-0.0).toLikeString())

    @Test
    fun smallValueInScientificNotation() = assertEquals("0.0001%", 1.0E-4.toLikeString())

    @Test
    fun tinyValueTruncatesToZero() = assertEquals("0.0000%", 1.5E-5.toLikeString())

    @Test
    fun tinyNegativeValue() = assertEquals("-0.0000%", (-1.0E-7).toLikeString())
}
