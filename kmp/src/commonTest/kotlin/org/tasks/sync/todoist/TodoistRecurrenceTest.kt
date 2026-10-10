package org.tasks.sync.todoist

import org.tasks.data.entity.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoistRecurrenceTest {
    @Test
    fun parsesSimpleFrequencies() {
        mapOf(
            "every day" to "FREQ=DAILY",
            "every week" to "FREQ=WEEKLY",
            "every month" to "FREQ=MONTHLY",
            "every year" to "FREQ=YEARLY",
        ).forEach { (input, rrule) ->
            assertEquals(rrule, TodoistRecurrence.fromString(input)?.recur?.toString(), input)
        }
    }

    @Test
    fun parsesIntervals() {
        mapOf(
            "every 3 days" to "FREQ=DAILY;INTERVAL=3",
            "every 2 weeks" to "FREQ=WEEKLY;INTERVAL=2",
            "every 6 months" to "FREQ=MONTHLY;INTERVAL=6",
            "every other week" to "FREQ=WEEKLY;INTERVAL=2",
        ).forEach { (input, rrule) ->
            assertEquals(rrule, TodoistRecurrence.fromString(input)?.recur?.toString(), input)
        }
    }

    @Test
    fun parsesWeekdays() {
        mapOf(
            "every mon" to "FREQ=WEEKLY;BYDAY=MO",
            "every monday" to "FREQ=WEEKLY;BYDAY=MO",
            "every mon, wed" to "FREQ=WEEKLY;BYDAY=MO,WE",
            "every 2 weeks on mon" to "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO",
            "every workday" to "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
        ).forEach { (input, rrule) ->
            assertEquals(rrule, TodoistRecurrence.fromString(input)?.recur?.toString(), input)
        }
    }

    @Test
    fun parsesDayOfMonth() {
        assertEquals(
            "FREQ=MONTHLY;BYMONTHDAY=15",
            TodoistRecurrence.fromString("every 15th")?.recur?.toString(),
        )
        assertEquals(
            "FREQ=MONTHLY;BYMONTHDAY=-1",
            TodoistRecurrence.fromString("every last day")?.recur?.toString(),
        )
    }

    @Test
    fun bangPrefixMeansRepeatFromCompletion() {
        assertEquals(
            Task.RepeatFrom.COMPLETION_DATE,
            TodoistRecurrence.fromString("every! 3 days")?.repeatFrom,
        )
        assertEquals(
            Task.RepeatFrom.DUE_DATE,
            TodoistRecurrence.fromString("every 3 days")?.repeatFrom,
        )
        assertEquals(
            "FREQ=DAILY;INTERVAL=3",
            TodoistRecurrence.fromString("every! 3 days")?.recur?.toString(),
        )
    }

    @Test
    fun returnsNullForUnrecognisedInput() {
        listOf(
            null,
            "",
            "   ",
            "tomorrow",
            "every 3rd friday",
            "every 2 months on mon",
            "every 0 days",
            "every blursday",
            "every 15th of blursday",
        ).forEach {
            assertNull(TodoistRecurrence.fromString(it), "expected null for $it")
        }
    }

    @Test
    fun serializesSimpleFrequencies() {
        mapOf(
            "FREQ=DAILY" to "every day",
            "FREQ=DAILY;INTERVAL=3" to "every 3 days",
            "FREQ=WEEKLY" to "every week",
            "FREQ=WEEKLY;INTERVAL=2" to "every 2 weeks",
            "FREQ=MONTHLY" to "every month",
            "FREQ=YEARLY" to "every year",
            "FREQ=WEEKLY;BYDAY=MO,WE" to "every mon, wed",
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO" to "every 2 weeks on mon",
            "FREQ=MONTHLY;BYMONTHDAY=15" to "every 15th",
            "FREQ=MONTHLY;BYMONTHDAY=-1" to "every last day",
            "FREQ=MONTHLY;BYMONTHDAY=1" to "every 1st",
            "FREQ=MONTHLY;BYMONTHDAY=2" to "every 2nd",
            "FREQ=MONTHLY;BYMONTHDAY=3" to "every 3rd",
            "FREQ=MONTHLY;BYMONTHDAY=11" to "every 11th",
        ).forEach { (rrule, expected) ->
            assertEquals(
                expected,
                TodoistRecurrence.toString(
                    org.tasks.repeats.Recur.parse(rrule),
                    Task.RepeatFrom.DUE_DATE,
                ),
                rrule,
            )
        }
    }

    @Test
    fun serializesRepeatFromCompletionWithBang() {
        assertEquals(
            "every! 3 days",
            TodoistRecurrence.toString(
                org.tasks.repeats.Recur.parse("FREQ=DAILY;INTERVAL=3"),
                Task.RepeatFrom.COMPLETION_DATE,
            ),
        )
    }

    @Test
    fun refusesToApproximateRulesTodoistCannotExpress() {
        listOf(
            "FREQ=DAILY;COUNT=10",
            "FREQ=DAILY;UNTIL=20261231",
            "FREQ=MONTHLY;BYDAY=3MO",
            "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1",
            "FREQ=HOURLY",
            "FREQ=MINUTELY",
            "FREQ=YEARLY;BYMONTH=4;BYMONTHDAY=1",
            "FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=15",
            "FREQ=DAILY;BYHOUR=9",
        ).forEach { rrule ->
            assertNull(
                TodoistRecurrence.toString(
                    org.tasks.repeats.Recur.parse(rrule),
                    Task.RepeatFrom.DUE_DATE,
                ),
                "expected null for $rrule",
            )
        }
    }

    @Test
    fun roundTripsThroughBothDirections() {
        listOf(
            "FREQ=DAILY",
            "FREQ=DAILY;INTERVAL=4",
            "FREQ=WEEKLY",
            "FREQ=WEEKLY;INTERVAL=3",
            "FREQ=WEEKLY;BYDAY=MO,WE,FR",
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU",
            "FREQ=MONTHLY",
            "FREQ=MONTHLY;BYMONTHDAY=9",
            "FREQ=MONTHLY;BYMONTHDAY=-1",
            "FREQ=YEARLY",
            "FREQ=YEARLY;INTERVAL=2",
        ).forEach { rrule ->
            val string = TodoistRecurrence.toString(
                org.tasks.repeats.Recur.parse(rrule),
                Task.RepeatFrom.DUE_DATE,
            )
            assertEquals(rrule, TodoistRecurrence.fromString(string)?.recur?.toString(), "$rrule -> $string")
        }
    }
}
