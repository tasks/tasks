package org.tasks.time

import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.TzId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class DateTimeJvmTest {
    private val berlin = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone("Europe/Berlin")

    private fun withTZ(id: String, block: () -> Unit) {
        val default = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(id))
            block()
        } finally {
            java.util.TimeZone.setDefault(default)
        }
    }

    @Test
    fun toDateTimeKeepsTheInstant() {
        val dateTime = DateTime(2015, 1, 6, 10, 30)
        assertEquals(dateTime.millis, dateTime.toDateTime().time)
        assertThrows(IllegalStateException::class.java) { DateTime(0).toDateTime() }
    }

    @Test
    fun toDateIsTheLocalDateAsUtc() = withTZ("America/Chicago") {
        val date = DateTime(2015, 1, 6, 10, 30).toDate()
        assertEquals("20150106", date.toString())
        assertEquals(DateTime(2015, 1, 6, timeZone = DateTime.UTC).millis, date.time)
        assertThrows(IllegalStateException::class.java) { DateTime(0).toDate() }
    }

    @Test
    fun toLocalDate() {
        assertEquals(LocalDate.of(2015, 1, 6), DateTime(2015, 1, 6, 10, 30).toLocalDate())
        assertNull(DateTime(0).toLocalDate())
    }

    @Test
    fun toLocalDateTimeHasMinutePrecision() {
        assertEquals(LocalDateTime.of(2015, 1, 6, 10, 30), DateTime(2015, 1, 6, 10, 30, 45, 123).toLocalDateTime())
        assertNull(DateTime(0).toLocalDateTime())
    }

    @Test
    fun weekDay() {
        val sunday = DateTime(2024, 12, 22)
        assertEquals(
            listOf(WeekDay.SU, WeekDay.MO, WeekDay.TU, WeekDay.WE, WeekDay.TH, WeekDay.FR, WeekDay.SA),
            (0..6).map { sunday.plusDays(it).weekDay },
        )
    }

    @Test
    fun fromNullDate() {
        assertEquals(0L, DateTime.from(null as Date?).millis)
        assertEquals(0L, DateTime.from(null as net.fortuna.ical4j.model.Date?).millis)
    }

    @Test
    fun fromDateReadsUtcFieldsAsLocal() = withTZ("America/Chicago") {
        val utcMidnight = Date(DateTime(2015, 1, 6, timeZone = DateTime.UTC).millis)
        assertEquals(DateTime(2015, 1, 6), DateTime.from(utcMidnight))
        assertEquals(DateTime(2015, 7, 6), DateTime.from(DateTime(2015, 7, 6).toDate()))
    }

    @Test
    fun fromUtcIcal4jDateTime() {
        val dateTime = DateTime.from(net.fortuna.ical4j.model.DateTime("20150106T160000Z"))
        assertEquals(DateTime.UTC, dateTime.timeZone)
        assertEquals(DateTime(2015, 1, 6, 16, 0, timeZone = DateTime.UTC), dateTime)
    }

    @Test
    fun fromFloatingIcal4jDateTime() = withTZ("America/Chicago") {
        assertEquals(DateTime(2015, 1, 6, 10, 0), DateTime.from(net.fortuna.ical4j.model.DateTime("20150106T100000")))
    }

    @Test
    fun fromIcal4jDateTimeWithIanaTzid() {
        val dateTime = DateTime.from(net.fortuna.ical4j.model.DateTime("20240101T120000", berlin))
        assertEquals(DateTime(2024, 1, 1, 11, 0, timeZone = DateTime.UTC).millis, dateTime.millis)
        assertEquals(12, dateTime.hourOfDay)
    }

    @Test
    fun fromIcal4jDateTimeWithWindowsTzidUsesTheVTimeZoneRules() {
        val berlinRulesUnderWindowsTzid = VTimeZone(berlin.vTimeZone.properties, berlin.vTimeZone.observances).apply {
            properties.remove(properties.getProperty<Property>(Property.TZID))
            properties.add(TzId("W. Europe Standard Time"))
        }
        val tz = net.fortuna.ical4j.model.TimeZone(berlinRulesUnderWindowsTzid)
        val dateTime = DateTime.from(net.fortuna.ical4j.model.DateTime("20240101T120000", tz))
        assertEquals(DateTime(2024, 1, 1, 11, 0, timeZone = DateTime.UTC).millis, dateTime.millis)
        assertEquals(12, dateTime.hourOfDay)
    }
}
