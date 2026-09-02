package org.tasks.api

import android.content.ContentValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import java.util.concurrent.TimeUnit

class TasksApiWriteTest : ApiTestCase() {
    @Test
    fun createReturnsAnItemUri() {
        val uri = resolver.insert(
            uri(Tasks.PATH),
            ContentValues().apply { put(Tasks.TITLE, "Renew passport") },
        )!!
        assertEquals(Tasks.TYPE_ITEM, resolver.getType(uri))
        assertEquals("Renew passport", resolver.query(uri, null, null, null, null)!!.string(Tasks.TITLE))
    }

    @Test
    fun updateIsAPatch() {
        val id = newTask("original", Tasks.NOTES to "keep me", Tasks.DUE_DATE to day(1))
        val due = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

        assertEquals(1, update(Tasks.PATH, id, Tasks.TITLE to "renamed"))

        val cursor = resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("renamed", it.getString(it.getColumnIndexOrThrow(Tasks.TITLE)))
            assertEquals("keep me", it.getString(it.getColumnIndexOrThrow(Tasks.NOTES)))
            assertEquals(due, it.getLong(it.getColumnIndexOrThrow(Tasks.DUE_DATE)))
        }
    }

    @Test
    fun nullAndTheEmptyValueBothClear() {
        val id = newTask("t", Tasks.NOTES to "notes", Tasks.DUE_DATE to day(1))

        update(Tasks.PATH, id, Tasks.NOTES to null)
        update(Tasks.PATH, id, Tasks.DUE_DATE to 0L)

        val cursor = resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(it.getColumnIndexOrThrow(Tasks.NOTES)))
            assertEquals(0L, it.getLong(it.getColumnIndexOrThrow(Tasks.DUE_DATE)))
        }
    }

    @Test
    fun allDayFlagSurvivesARoundTrip() {
        val id = newTask("t", Tasks.DUE_DATE to currentTimeMillis(), Tasks.DUE_ALL_DAY to 1)

        val due: Long
        val allDay: Int
        resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.use {
            it.moveToFirst()
            due = it.getLong(it.getColumnIndexOrThrow(Tasks.DUE_DATE))
            allDay = it.getInt(it.getColumnIndexOrThrow(Tasks.DUE_ALL_DAY))
        }
        assertEquals(1, allDay)

        update(Tasks.PATH, id, Tasks.DUE_DATE to due, Tasks.DUE_ALL_DAY to allDay)
        resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.use {
            it.moveToFirst()
            assertEquals(due, it.getLong(it.getColumnIndexOrThrow(Tasks.DUE_DATE)))
            assertEquals(1, it.getInt(it.getColumnIndexOrThrow(Tasks.DUE_ALL_DAY)))
        }

        update(Tasks.PATH, id, Tasks.DUE_ALL_DAY to 0)
        assertEquals(0, resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.int(Tasks.DUE_ALL_DAY))
    }

    @Test
    fun anAbsentAllDayFlagMeansTimed() {
        val wholeMinute = (currentTimeMillis() / 60_000L) * 60_000L
        val id = newTask("t", Tasks.DUE_DATE to wholeMinute)

        assertEquals(0, query(Tasks.PATH, "?_id=$id").int(Tasks.DUE_ALL_DAY))
    }

    @Test
    fun anAllDayDateIsTheLocalDate() {
        val id = newTask("t", Tasks.DUE_DATE to day(1), Tasks.DUE_ALL_DAY to 1)

        val due = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)
        assertEquals(1, query(Tasks.PATH, "?_id=$id").int(Tasks.DUE_ALL_DAY))
        assertEquals(day(1).startOfDay(), due.startOfDay())
    }

    @Test
    fun aRecurrenceIsStoredAsTheBareRule() {
        val id = newTask("t", Tasks.RECURRENCE to "RRULE:FREQ=WEEKLY;BYDAY=MO")

        assertEquals("FREQ=WEEKLY;BYDAY=MO", query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE))
    }

    @Test
    fun anUnparseableRecurrenceIsRejected() {
        assertThrows<IllegalArgumentException> { newTask("t", Tasks.RECURRENCE to "every tuesday") }
    }

    @Test
    fun aNameCannotBeCleared() = runBlockingTest {
        val task = newTask("t")
        val tag = insert(Tags.PATH, Tags.NAME to "admin")
        val list = newList("Groceries")

        listOf("", null).forEach { empty ->
            assertThrows<IllegalArgumentException> { update(Tasks.PATH, task, Tasks.TITLE to empty) }
            assertThrows<IllegalArgumentException> { update(Tags.PATH, tag, Tags.NAME to empty) }
            assertThrows<IllegalArgumentException> { update(Lists.PATH, list, Lists.TITLE to empty) }
            assertThrows<IllegalArgumentException> { insert(Tasks.PATH, Tasks.TITLE to empty) }
            assertThrows<IllegalArgumentException> { insert(Tags.PATH, Tags.NAME to empty) }
            assertThrows<IllegalArgumentException> {
                insert(Lists.PATH, Lists.ACCOUNT_ID to account().id, Lists.TITLE to empty)
            }
        }

        assertEquals("t", query(Tasks.PATH, "?_id=$task").string(Tasks.TITLE))
        assertEquals("admin", query(Tags.PATH, "?_id=$tag").string(Tags.NAME))
        assertEquals("Groceries", query(Lists.PATH, "?_id=$list").string(Lists.TITLE))
    }

    @Test
    fun theSetValuedColumnsAreReadOnly() {
        val id = newTask("t")
        listOf(Tasks.TAG_IDS, Tasks.CHILD_COUNT, Tasks.UNCOMPLETED_CHILD_COUNT)
            .forEach { column ->
                assertThrows<IllegalArgumentException> { update(Tasks.PATH, id, column to "x") }
                assertThrows<IllegalArgumentException> { newTask("other", column to "x") }
            }
    }

    @Test
    fun ifModifiedAtGuardsAgainstAConcurrentEdit() {
        val id = newTask("t")
        val modified = resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.long(Tasks.MODIFIED_AT)

        assertEquals(1, update(Tasks.PATH, id, Tasks.TITLE to "first", query = "?if_modified_at=$modified"))
        assertEquals(0, update(Tasks.PATH, id, Tasks.TITLE to "second", query = "?if_modified_at=$modified"))
        assertEquals("first", query(Tasks.PATH).string(Tasks.TITLE))
    }

    @Test
    fun updatingAMissingTaskReturnsZero() {
        assertEquals(0, update(Tasks.PATH, 9999, Tasks.TITLE to "nope"))
    }

    @Test
    fun completingCascadesToSubtasks() {
        val parent = newTask("parent")
        val child = newTask("child", Tasks.PARENT_ID to parent)

        update(Tasks.PATH, parent, Tasks.COMPLETED_AT to currentTimeMillis())

        assertEquals(2, query(Tasks.PATH, "?completed_after=0").rows())
        assertTrue(resolver.query(itemUri(Tasks.PATH, child), null, null, null, null)!!.long(Tasks.COMPLETED_AT) > 0)
    }

    @Test
    fun aTaskCanBeCreatedAlreadyCompleted() {
        val when_ = currentTimeMillis() - TimeUnit.DAYS.toMillis(3)

        newTask("imported", Tasks.COMPLETED_AT to when_)

        assertEquals(when_, query(Tasks.PATH).long(Tasks.COMPLETED_AT))
        assertEquals(listOf("imported"), query(Tasks.PATH, "?completed_after=0").strings(Tasks.TITLE))
    }

    @Test
    fun creatingACompletedRecurringTaskAdvancesTheSeries() {
        val id = newTask(
            "standup",
            Tasks.DUE_DATE to currentTimeMillis(),
            Tasks.DUE_ALL_DAY to 1,
            Tasks.RECURRENCE to "RRULE:FREQ=DAILY",
            Tasks.COMPLETED_AT to currentTimeMillis(),
        )

        assertEquals(0L, resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.long(Tasks.COMPLETED_AT))
    }

    @Test
    fun creatingWithCompletedAtZeroLeavesTheTaskOpen() {
        newTask("open", Tasks.COMPLETED_AT to 0L)

        assertEquals(0L, query(Tasks.PATH).long(Tasks.COMPLETED_AT))
    }

    @Test
    fun completingWithATimestampUsesIt() {
        val id = newTask("t")
        val when_ = currentTimeMillis() - TimeUnit.DAYS.toMillis(2)

        update(Tasks.PATH, id, Tasks.COMPLETED_AT to when_)

        assertEquals(when_, query(Tasks.PATH).long(Tasks.COMPLETED_AT))
    }

    @Test
    fun completingARecurringTaskAdvancesItInstead() {
        val due = currentTimeMillis()
        val id = newTask(
            "standup",
            Tasks.DUE_DATE to due,
            Tasks.DUE_ALL_DAY to 1,
            Tasks.RECURRENCE to "RRULE:FREQ=DAILY",
        )
        val before = query(Tasks.PATH).long(Tasks.DUE_DATE)

        assertEquals(1, update(Tasks.PATH, id, Tasks.COMPLETED_AT to currentTimeMillis()))

        resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(0L, it.getLong(it.getColumnIndexOrThrow(Tasks.COMPLETED_AT)))
            assertTrue(it.getLong(it.getColumnIndexOrThrow(Tasks.DUE_DATE)) > before)
        }
    }

    @Test
    fun deletingATaskTakesItsSubtasks() {
        val parent = newTask("parent")
        newTask("child", Tasks.PARENT_ID to parent)

        assertEquals(1, delete(Tasks.PATH, parent))
        assertEquals(0, query(Tasks.PATH).rows())
    }

    @Test
    fun aReadOnlyListRefusesWrites() {
        val readOnly = runBlockingTest { readOnlyList() }

        assertThrows<UnsupportedOperationException> {
            insert(Tasks.PATH, Tasks.TITLE to "nope", Tasks.LIST_ID to readOnly)
        }
    }

    @Test
    fun movingATaskChangesItsList() {
        val other = runBlockingTest { newList("Other") }
        val id = newTask("movable")
        assertEquals(listId, query(Tasks.PATH).long(Tasks.LIST_ID))

        assertEquals(1, update(Tasks.PATH, id, Tasks.LIST_ID to other))

        assertEquals(other, query(Tasks.PATH).long(Tasks.LIST_ID))
        assertEquals(listOf("movable"), query(Tasks.PATH, "?list_id=$other").strings(Tasks.TITLE))
    }

    @Test
    fun alarmsSplitTheStoredUnion() {
        val task = newTask("t")
        val absolute = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_DATE_TIME,
            Alarms.TRIGGER_AT to 123456789L,
        )
        val relative = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -TimeUnit.HOURS.toMillis(2),
        )

        resolver.query(itemUri(Alarms.PATH, absolute), null, null, null, null)!!.use {
            it.moveToFirst()
            assertEquals(123456789L, it.getLong(it.getColumnIndexOrThrow(Alarms.TRIGGER_AT)))
            assertEquals(0L, it.getLong(it.getColumnIndexOrThrow(Alarms.OFFSET_MS)))
        }
        resolver.query(itemUri(Alarms.PATH, relative), null, null, null, null)!!.use {
            it.moveToFirst()
            assertEquals(0L, it.getLong(it.getColumnIndexOrThrow(Alarms.TRIGGER_AT)))
            assertEquals(-TimeUnit.HOURS.toMillis(2), it.getLong(it.getColumnIndexOrThrow(Alarms.OFFSET_MS)))
        }
    }

    @Test
    fun identicalAlarmsAreNotDuplicated() {
        val task = newTask("t")
        val first = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -1000L,
        )
        val second = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -1000L,
        )

        assertEquals(first, second)
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun updatingAnAlarmKeepsItsId() {
        val task = newTask("t")
        val id = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -1000L,
        )

        assertEquals(1, update(Alarms.PATH, id, Alarms.OFFSET_MS to -2000L))

        assertEquals(-2000L, resolver.query(itemUri(Alarms.PATH, id), null, null, null, null)!!.long(Alarms.OFFSET_MS))
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun theWrongTimeColumnForTheTypeIsRejected() {
        val task = newTask("t")

        assertThrows<IllegalArgumentException> {
            insert(
                Alarms.PATH,
                Alarms.TASK_ID to task,
                Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
                Alarms.TRIGGER_AT to 1000L,
            )
        }
    }

    @Test
    fun alarmsAreFilteredByType() {
        val task = newTask("t")
        insert(Alarms.PATH, Alarms.TASK_ID to task, Alarms.TYPE to Alarms.TYPE_SNOOZE, Alarms.TRIGGER_AT to 999L)
        insert(Alarms.PATH, Alarms.TASK_ID to task, Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE, Alarms.OFFSET_MS to -1L)

        assertEquals(listOf(task), query(Alarms.PATH, "?type=snooze").longs(Alarms.TASK_ID))
        assertEquals(2, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun aTagIsOnATaskOnceOrNotAtAll() {
        val task = newTask("t")
        insert(Tags.PATH, Tags.NAME to "admin")
        val tagId = query(Tags.PATH).long(TasksContract.ID)

        val first = insert(TaskTags.PATH, TaskTags.TASK_ID to task, TaskTags.TAG_ID to tagId)
        val second = insert(TaskTags.PATH, TaskTags.TASK_ID to task, TaskTags.TAG_ID to tagId)

        assertEquals(first, second)
        assertEquals("$tagId", query(Tasks.PATH, "?_id=$task").string(Tasks.TAG_IDS))
    }

    @Test
    fun aTaskTagIsDeletableByItsNaturalKey() {
        val task = newTask("t")
        insert(Tags.PATH, Tags.NAME to "admin")
        val tagId = query(Tags.PATH).long(TasksContract.ID)
        insert(TaskTags.PATH, TaskTags.TASK_ID to task, TaskTags.TAG_ID to tagId)

        val deleted = resolver.delete(
            uri(TaskTags.PATH, "?task_id=$task&tag_id=$tagId"), null, null,
        )

        assertEquals(1, deleted)
        assertEquals(0, query(TaskTags.PATH).rows())
    }

    @Test
    fun aTagOnAnUnknownUidIsRejected() {
        val task = newTask("t")
        assertThrows<IllegalArgumentException> {
            insert(TaskTags.PATH, TaskTags.TASK_ID to task, TaskTags.TAG_ID to "nope")
        }
    }

    @Test
    fun tagNamesAreUniqueCaseInsensitively() {
        val first = insert(Tags.PATH, Tags.NAME to "Admin", Tags.COLOR to 42)
        val second = insert(Tags.PATH, Tags.NAME to "ADMIN", Tags.COLOR to 99)

        assertEquals(first, second)
        assertEquals(42, query(Tags.PATH).int(Tags.COLOR))
    }

    @Test
    fun tagsCanBeRenamedAndDeleted() {
        val id = insert(Tags.PATH, Tags.NAME to "admin")
        val task = newTask("t")
        val tagId = query(Tags.PATH).long(TasksContract.ID)
        insert(TaskTags.PATH, TaskTags.TASK_ID to task, TaskTags.TAG_ID to tagId)

        assertEquals(1, update(Tags.PATH, id, Tags.NAME to "ops"))
        assertEquals("ops", query(Tags.PATH).string(Tags.NAME))

        assertEquals(1, delete(Tags.PATH, id))
        assertEquals(0, query(Tags.PATH).rows())
        assertEquals(0, query(TaskTags.PATH).rows())
    }

    @Test
    fun placesAreUniqueByCoordinate() {
        val first = insert(
            Places.PATH,
            Places.NAME to "Home",
            Places.LATITUDE to 51.5074,
            Places.LONGITUDE to -0.1278,
        )
        val second = insert(
            Places.PATH,
            Places.NAME to "Home again",
            Places.LATITUDE to 51.5074,
            Places.LONGITUDE to -0.1278,
        )

        assertEquals(first, second)
        assertEquals("Home", query(Places.PATH).string(Places.NAME))
    }

    @Test
    fun placesRequireCoordinates() {
        assertThrows<IllegalArgumentException> { insert(Places.PATH, Places.NAME to "nowhere") }
    }

    @Test
    fun displayNameFallsBackToTheAddress() {
        insert(
            Places.PATH,
            Places.ADDRESS to "221B Baker Street",
            Places.LATITUDE to 51.5237,
            Places.LONGITUDE to -0.1585,
        )

        assertEquals("221B Baker Street", query(Places.PATH).string(Places.DISPLAY_NAME))
    }

    @Test
    fun aTimedDateIsReportedOnTheMinute() {
        val id = newTask("t", Tasks.DUE_DATE to day(1), Tasks.DUE_ALL_DAY to 0)

        val due = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

        assertEquals(0L, due % 60_000)
        assertEquals(0, query(Tasks.PATH, "?_id=$id").int(Tasks.DUE_ALL_DAY))
    }

    @Test
    fun writingBackAReadTimedDateChangesNothing() {
        val id = newTask("t", Tasks.DUE_DATE to day(1), Tasks.DUE_ALL_DAY to 0)
        val due = query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE)

        update(Tasks.PATH, id, Tasks.DUE_DATE to due, Tasks.DUE_ALL_DAY to 0)

        assertEquals(due, query(Tasks.PATH, "?_id=$id").long(Tasks.DUE_DATE))
        assertEquals(0, query(Tasks.PATH, "?_id=$id").int(Tasks.DUE_ALL_DAY))
    }

    @Test
    fun aTimedStartDateIsAlsoReportedOnTheMinute() {
        val id = newTask("t", Tasks.START_DATE to day(1), Tasks.START_ALL_DAY to 0)

        val start = query(Tasks.PATH, "?_id=$id").long(Tasks.START_DATE)

        assertEquals(0L, start % 60_000)
        assertEquals(0, query(Tasks.PATH, "?_id=$id").int(Tasks.START_ALL_DAY))
    }

    @Test
    fun filingATaskAtAPlaceCreatesNoReminder() {
        val task = newTask("t")
        val placeId = newPlace()

        update(Tasks.PATH, task, Tasks.PLACE_ID to placeId)

        assertEquals(placeId, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
        assertEquals(listOf("t"), query(Tasks.PATH, "?place_id=$placeId").strings(Tasks.TITLE))

        assertEquals(0, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun aLocationReminderSetsThePlaceAndAppearsAsAnAlarm() {
        val task = newTask("t")
        val placeId = newPlace()

        val id = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )

        assertEquals(placeId, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(
            Alarms.TYPE_LOCATION_ARRIVAL,
            query(Alarms.PATH, "?task_id=$task").string(Alarms.TYPE),
        )
        assertEquals(placeId, query(Alarms.PATH, "?task_id=$task").long(Alarms.PLACE_ID))

        assertEquals(id, resolver.query(itemUri(Alarms.PATH, id), null, null, null, null)!!
            .long(TasksContract.ID))
    }

    @Test
    fun arrivalAndDepartureAreIndependentReminders() {
        val task = newTask("t")
        val placeId = newPlace()

        val arrival = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )
        val departure = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_DEPARTURE,
            Alarms.PLACE_ID to placeId,
        )

        assertTrue(arrival != departure)
        assertEquals(2, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(
            listOf(Alarms.TYPE_LOCATION_ARRIVAL, Alarms.TYPE_LOCATION_DEPARTURE),
            query(Alarms.PATH, "?task_id=$task").strings(Alarms.TYPE),
        )

        assertEquals(1, delete(Alarms.PATH, arrival))
        assertEquals(
            listOf(Alarms.TYPE_LOCATION_DEPARTURE),
            query(Alarms.PATH, "?task_id=$task").strings(Alarms.TYPE),
        )
        assertEquals(placeId, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))

        assertEquals(1, delete(Alarms.PATH, departure))
        assertEquals(0, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(placeId, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
    }

    @Test
    fun aRepeatedLocationReminderIsIdempotent() {
        val task = newTask("t")
        val placeId = newPlace()

        val first = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )
        val second = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )

        assertEquals(first, second)
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun deletingALocationReminderTwiceReportsNoSecondChange() {
        val task = newTask("t")
        val placeId = newPlace()
        val id = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )

        assertEquals(1, delete(Alarms.PATH, id))
        assertEquals(0, delete(Alarms.PATH, id))
    }

    @Test
    fun aLocationReminderNeedsAPlace() {
        val task = newTask("t")
        assertThrows<IllegalArgumentException> {
            insert(
                Alarms.PATH,
                Alarms.TASK_ID to task,
                Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            )
        }
    }

    @Test
    fun aTimeAlarmCannotCarryAPlace() {
        val task = newTask("t")
        val placeId = newPlace()
        assertThrows<IllegalArgumentException> {
            insert(
                Alarms.PATH,
                Alarms.TASK_ID to task,
                Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
                Alarms.OFFSET_MS to -1000L,
                Alarms.PLACE_ID to placeId,
            )
        }
    }

    @Test
    fun aLocationReminderCannotCarryATime() {
        val task = newTask("t")
        val placeId = newPlace()
        assertThrows<IllegalArgumentException> {
            insert(
                Alarms.PATH,
                Alarms.TASK_ID to task,
                Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
                Alarms.PLACE_ID to placeId,
                Alarms.OFFSET_MS to -1000L,
            )
        }
    }

    @Test
    fun aLocationReminderHasNothingToUpdate() {
        val task = newTask("t")
        val placeId = newPlace()
        val id = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )

        assertThrows<IllegalArgumentException> {
            update(Alarms.PATH, id, Alarms.OFFSET_MS to 5L)
        }
    }

    @Test
    fun aSecondPlaceOnTheSameTaskIsRejected() {
        val task = newTask("t")
        val home = newPlace(name = "Home", latitude = 1.0)
        val office = newPlace(name = "Office", latitude = 2.0)
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to home,
        )

        assertThrows<IllegalArgumentException> {
            insert(
                Alarms.PATH,
                Alarms.TASK_ID to task,
                Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
                Alarms.PLACE_ID to office,
            )
        }
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(home, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
    }

    @Test
    fun aTaskCanBeFiledUnderAPlaceAndMovedAndCleared() {
        val home = newPlace(name = "Home", latitude = 1.0)
        val office = newPlace(name = "Office", latitude = 2.0)
        val task = newTask("t", Tasks.PLACE_ID to home)

        assertEquals(home, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
        assertEquals(0, query(Alarms.PATH, "?task_id=$task").rows())

        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_DEPARTURE,
            Alarms.PLACE_ID to home,
        )

        assertEquals(1, update(Tasks.PATH, task, Tasks.PLACE_ID to office))
        assertEquals(office, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
        assertEquals(1, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(office, query(Alarms.PATH, "?task_id=$task").long(Alarms.PLACE_ID))

        assertEquals(1, update(Tasks.PATH, task, Tasks.PLACE_ID to 0L))
        assertEquals(0L, query(Tasks.PATH, "?_id=$task").long(Tasks.PLACE_ID))
        assertEquals(0, query(Alarms.PATH, "?task_id=$task").rows())
    }

    @Test
    fun locationRemindersAreFilterableByTypeAndPlace() {
        val task = newTask("t")
        val placeId = newPlace()
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -TimeUnit.HOURS.toMillis(2),
        )

        assertEquals(2, query(Alarms.PATH, "?task_id=$task").rows())
        assertEquals(
            listOf(Alarms.TYPE_LOCATION_ARRIVAL),
            query(Alarms.PATH, "?type=${Alarms.TYPE_LOCATION_ARRIVAL}").strings(Alarms.TYPE),
        )
        assertEquals(
            listOf(Alarms.TYPE_RELATIVE_DUE),
            query(Alarms.PATH, "?type=${Alarms.TYPE_RELATIVE_DUE}").strings(Alarms.TYPE),
        )

        assertEquals(
            listOf(Alarms.TYPE_LOCATION_ARRIVAL),
            query(Alarms.PATH, "?place_id=$placeId").strings(Alarms.TYPE),
        )
    }

    @Test
    fun countingRemindersSpansTheUnion() {
        val task = newTask("t")
        val placeId = newPlace()
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to placeId,
        )
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_LOCATION_DEPARTURE,
            Alarms.PLACE_ID to placeId,
        )
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_DUE,
            Alarms.OFFSET_MS to -TimeUnit.HOURS.toMillis(2),
        )

        val counted = query(Alarms.PATH, "?task_id=$task&limit=0")
        assertEquals(0, counted.rows())
        assertEquals(3, counted.total())

        val first = query(Alarms.PATH, "?task_id=$task&limit=2").longs(TasksContract.ID)
        val second = query(Alarms.PATH, "?task_id=$task&limit=2&offset=2").longs(TasksContract.ID)
        assertEquals(2, first.size)
        assertEquals(1, second.size)
        assertEquals(3, (first + second).distinct().size)
    }

    @Test
    fun aLocalListCanBeCreatedRenamedAndDeleted() {
        val accountId = runBlockingTest { account().id }

        val id = insert(Lists.PATH, Lists.ACCOUNT_ID to accountId, Lists.TITLE to "Groceries")
        assertEquals(listOf("Groceries"), query(Lists.PATH, "?_id=$id").strings(Lists.TITLE))

        assertEquals(1, update(Lists.PATH, id, Lists.TITLE to "Shopping", Lists.COLOR to 7))
        resolver.query(itemUri(Lists.PATH, id), null, null, null, null)!!.use {
            it.moveToFirst()
            assertEquals("Shopping", it.getString(it.getColumnIndexOrThrow(Lists.TITLE)))
            assertEquals(7, it.getInt(it.getColumnIndexOrThrow(Lists.COLOR)))
        }

        assertEquals(1, delete(Lists.PATH, id))
        assertEquals(0, resolver.query(itemUri(Lists.PATH, id), null, null, null, null)!!.rows())
    }

    @Test
    fun creatingAListNeedsAnAccount() {
        assertThrows<IllegalArgumentException> { insert(Lists.PATH, Lists.TITLE to "orphan") }
        assertThrows<IllegalArgumentException> {
            insert(Lists.PATH, Lists.ACCOUNT_ID to 9999L, Lists.TITLE to "orphan")
        }
    }

    @Test
    fun aReadOnlyListCannotBeRenamed() {
        val readOnly = runBlockingTest { readOnlyList() }
        val id = readOnly

        assertThrows<UnsupportedOperationException> { update(Lists.PATH, id, Lists.TITLE to "nope") }
        assertThrows<UnsupportedOperationException> { delete(Lists.PATH, id) }
    }

    @Test
    fun aTaskCannotBeItsOwnParent() {
        val id = newTask("t")

        assertThrows<IllegalArgumentException> { update(Tasks.PATH, id, Tasks.PARENT_ID to id) }

        assertEquals(0L, query(Tasks.PATH, "?_id=$id").long(Tasks.PARENT_ID))
    }

    @Test
    fun aTaskCannotBeNestedUnderItsOwnSubtask() {
        val parent = newTask("parent")
        val child = newTask("child", Tasks.PARENT_ID to parent)
        val grandchild = newTask("grandchild", Tasks.PARENT_ID to child)

        assertThrows<IllegalArgumentException> {
            update(Tasks.PATH, parent, Tasks.PARENT_ID to child)
        }
        assertThrows<IllegalArgumentException> {
            update(Tasks.PATH, parent, Tasks.PARENT_ID to grandchild)
        }

        assertEquals(0L, query(Tasks.PATH, "?_id=$parent").long(Tasks.PARENT_ID))
        assertEquals(parent, query(Tasks.PATH, "?_id=$child").long(Tasks.PARENT_ID))
    }

    @Test
    fun aTaskAlwaysBelongsToAList() {
        val id = newTask("t")

        assertThrows<IllegalArgumentException> { update(Tasks.PATH, id, Tasks.LIST_ID to 0L) }
        assertThrows<IllegalArgumentException> { newTask("nope", Tasks.LIST_ID to 0L) }

        assertEquals(listId, query(Tasks.PATH, "?_id=$id").long(Tasks.LIST_ID))
    }

    @Test
    fun repeatsOnlyApplyToRelativeAlarms() {
        val task = newTask("t")

        listOf(Alarms.TYPE_DATE_TIME, Alarms.TYPE_SNOOZE).forEach { type ->
            assertThrows<IllegalArgumentException> {
                insert(
                    Alarms.PATH,
                    Alarms.TASK_ID to task,
                    Alarms.TYPE to type,
                    Alarms.TRIGGER_AT to day(1),
                    Alarms.REPEAT_COUNT to 6,
                    Alarms.INTERVAL_MS to TimeUnit.DAYS.toMillis(1),
                )
            }
        }

        val id = insert(
            Alarms.PATH,
            Alarms.TASK_ID to task,
            Alarms.TYPE to Alarms.TYPE_DATE_TIME,
            Alarms.TRIGGER_AT to day(1),
        )
        assertThrows<IllegalArgumentException> {
            update(Alarms.PATH, id, Alarms.REPEAT_COUNT to 6)
        }
        assertEquals(0, query(Alarms.PATH, "?task_id=$task").int(Alarms.REPEAT_COUNT))
    }

    private fun newPlace(name: String = "Home", latitude: Double = 51.5): Long = insert(
        Places.PATH,
        Places.NAME to name,
        Places.LATITUDE to latitude,
        Places.LONGITUDE to -0.1,
    )

    @Test
    fun anEmptyStringClearsNotes() {
        val id = newTask("t", Tasks.NOTES to "some notes")
        assertEquals("some notes", query(Tasks.PATH, "?_id=$id").string(Tasks.NOTES))

        assertEquals(1, update(Tasks.PATH, id, Tasks.NOTES to ""))

        assertEquals("", query(Tasks.PATH, "?_id=$id").string(Tasks.NOTES))
    }

    @Test
    fun anEmptyStringStopsATaskRepeating() {
        val id = newTask("t", Tasks.RECURRENCE to "FREQ=DAILY;COUNT=3")
        assertEquals("FREQ=DAILY;COUNT=3", query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE))

        assertEquals(1, update(Tasks.PATH, id, Tasks.RECURRENCE to ""))

        assertEquals("", query(Tasks.PATH, "?_id=$id").string(Tasks.RECURRENCE))
    }

    @Test
    fun anEmptyStringClearsAnIcon() {
        val tag = insert(Tags.PATH, Tags.NAME to "t", Tags.ICON to "luggage")
        assertEquals("luggage", query(Tags.PATH, "?_id=$tag").string(Tags.ICON))

        assertEquals(1, update(Tags.PATH, tag, Tags.ICON to ""))

        assertEquals("", query(Tags.PATH, "?_id=$tag").string(Tags.ICON))
    }
}
