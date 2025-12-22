package com.todoroo.astrid.gcal

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.tasks.TestUtilities.withTZ
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import org.tasks.time.DateTime
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidTest
class GCalHelperTest : InjectingTestCase() {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    @Inject lateinit var gcalHelper: GCalHelper

    private var testCalendarId: Long = -1

    @Before
    override fun setUp() {
        super.setUp()
        testCalendarId = createTestCalendar()
    }

    @After
    fun tearDown() {
        if (testCalendarId > 0) {
            try {
                val context = ApplicationProvider.getApplicationContext<Context>()
                context.contentResolver.delete(
                    ContentUris.withAppendedId(Calendars.CONTENT_URI, testCalendarId),
                    null,
                    null
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    @Test fun allDayEventInNewYork() = assertAllDayEvent("America/New_York") // UTC-5
    @Test fun allDayEventInBerlin() = assertAllDayEvent("Europe/Berlin") // UTC+1
    @Test fun allDayEventInAuckland() = assertAllDayEvent("Pacific/Auckland") // UTC+13
    @Test fun allDayEventInTokyo() = assertAllDayEvent("Asia/Tokyo") // UTC+9
    @Test fun allDayEventInHonolulu() = assertAllDayEvent("Pacific/Honolulu") // UTC-10
    @Test fun allDayEventInChatham() = assertAllDayEvent("Pacific/Chatham") // UTC+13:45

    private fun assertAllDayEvent(timezone: String) = withTZ(timezone) {
        val task = Task(dueDate = DateTime(2024, 12, 20).millis)

        val eventUri = gcalHelper.createTaskEvent(task, testCalendarId.toString())
            ?: throw RuntimeException("Event not created")

        val event = queryEvent(eventUri.toString()) ?: throw RuntimeException("Event not found")

        assertEquals(
            "DTSTART should be Dec 20 00:00 UTC",
            DateTime(2024, 12, 20, timeZone = DateTime.UTC).millis,
            event.dtStart
        )
        assertEquals(
            "DTEND should be Dec 21 00:00 UTC",
            DateTime(2024, 12, 21, timeZone = DateTime.UTC).millis,
            event.dtEnd
        )
    }

    private fun createTestCalendar(): Long {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, "test@test.com")
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, "Test Calendar")
            put(Calendars.CALENDAR_DISPLAY_NAME, "Test Calendar")
            put(Calendars.CALENDAR_COLOR, 0xFF0000)
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, "test@test.com")
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        }
        val uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, "test@test.com")
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val calendarUri = context.contentResolver.insert(uri, values)
        return ContentUris.parseId(calendarUri!!)
    }

    private fun queryEvent(eventUri: String): CalendarEvent? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cursor = context.contentResolver.query(
            eventUri.toUri(),
            arrayOf(
                Events.DTSTART,
                Events.DTEND,
                Events.ALL_DAY,
                Events.EVENT_TIMEZONE
            ),
            null,
            null,
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                CalendarEvent(
                    dtStart = it.getLong(0),
                    dtEnd = it.getLong(1),
                    allDay = it.getInt(2) == 1,
                    timezone = it.getString(3)
                )
            } else null
        }
    }

    private data class CalendarEvent(
        val dtStart: Long,
        val dtEnd: Long,
        val allDay: Boolean,
        val timezone: String?
    )
}
