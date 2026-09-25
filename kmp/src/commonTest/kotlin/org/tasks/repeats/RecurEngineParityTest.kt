package org.tasks.repeats

import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RecurEngineParityTest {
    @Test
    fun canonicalizesRulePartsTheSameWay() {
        mapOf(
            "FREQ=DAILY;BYHOUR=9,10,11,12,13,14,15,16;BYMINUTE=0,30" to
                    "FREQ=DAILY;BYHOUR=9,10,11,12,13,14,15,16;BYMINUTE=0,30",
            "FREQ=DAILY;COUNT=60;BYDAY=TU,TH;BYSETPOS=2" to
                    "FREQ=DAILY;COUNT=60;BYDAY=TU,TH;BYSETPOS=2",
            "FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13" to "FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR",
            "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1" to
                    "FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1",
            "FREQ=MONTHLY;INTERVAL=2;BYDAY=3MO" to "FREQ=MONTHLY;INTERVAL=2;BYDAY=3MO",
            "FREQ=MONTHLY;WKST=SU;INTERVAL=2;BYDAY=5TU" to "FREQ=MONTHLY;WKST=SU;INTERVAL=2;BYDAY=5TU",
            "FREQ=MONTHLY;UNTIL=20061220;INTERVAL=1;BYDAY=3WE" to
                    "FREQ=MONTHLY;UNTIL=20061220;INTERVAL=1;BYDAY=3WE",
            "FREQ=WEEKLY;WKST=MO;UNTIL=20160901T230000;INTERVAL=1;BYDAY=TH" to
                    "FREQ=WEEKLY;WKST=MO;UNTIL=20160901T230000;INTERVAL=1;BYDAY=TH",
            "FREQ=WEEKLY;COUNT=75;INTERVAL=2;BYDAY=SU,MO,TU;WKST=SU" to
                    "FREQ=WEEKLY;WKST=SU;COUNT=75;INTERVAL=2;BYDAY=SU,MO,TU",
            "FREQ=YEARLY;BYDAY=WE;BYWEEKNO=1,3,5" to "FREQ=YEARLY;BYWEEKNO=1,3,5;BYDAY=WE",
            "FREQ=YEARLY;COUNT=4;INTERVAL=2;BYMONTH=1,2,3;BYMONTHDAY=-1" to
                    "FREQ=YEARLY;COUNT=4;INTERVAL=2;BYMONTH=1,2,3;BYMONTHDAY=-1",
            "FREQ=YEARLY;BYMONTH=4;BYDAY=1SU" to "FREQ=YEARLY;BYMONTH=4;BYDAY=1SU",
            "FREQ=YEARLY;BYMONTHDAY=29;INTERVAL=1" to "FREQ=YEARLY;INTERVAL=1;BYMONTHDAY=29",
            "FREQ=MONTHLY;BYMONTH=2;BYMONTHDAY=30;INTERVAL=1" to
                    "FREQ=MONTHLY;INTERVAL=1;BYMONTH=2;BYMONTHDAY=30",
            "FREQ=MONTHLY;WKST=MO;INTERVAL=1;BYMONTH=2,3,9,10;BYMONTHDAY=28,29,30,31;BYSETPOS=-1" to
                    "FREQ=MONTHLY;WKST=MO;INTERVAL=1;BYMONTH=2,3,9,10;BYMONTHDAY=28,29,30,31;BYSETPOS=-1",
            "FREQ=DAILY;COUNT=0" to "FREQ=DAILY",
        ).forEach { (rule, canonical) ->
            assertEquals(canonical, Recur.parse(rule).toString(), rule)
        }
    }

    @Test
    fun rejectsRulesNeitherEngineAccepts() {
        listOf(
            "FREQ=WEEKLY;BYDAY=xx",
            "FREQ=FORTNIGHTLY;BYDAY=MO",
            "FREQ=YEARLY;BYMONTH=0",
            "FREQ=YEARLY;BYMONTHDAY=-400",
            "FREQ",
            "",
        ).forEach {
            assertFailsWith<IllegalArgumentException>(it) { Recur.parse(it) }
        }
    }

    @Test
    fun computesTheSameNextOccurrence() {
        val jan31 = DateTime(2026, 1, 31, 9, 0)
        val feb27 = DateTime(2026, 2, 27, 9, 0)

        assertEquals(DateTime(2026, 3, 31, 9, 0), next("FREQ=MONTHLY;BYMONTHDAY=31", jan31))
        assertEquals(DateTime(2026, 3, 31, 9, 0), next("FREQ=MONTHLY", jan31))
        assertEquals(DateTime(2026, 3, 16, 9, 0), next("FREQ=MONTHLY;INTERVAL=2;BYDAY=3MO", jan31))
        assertEquals(
            DateTime(2028, 2, 29, 9, 0),
            next("FREQ=YEARLY;BYMONTHDAY=29;BYMONTH=2", DateTime(2024, 2, 29, 9, 0)),
        )
        assertEquals(
            DateTime(2026, 3, 31, 9, 0),
            next("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1", feb27),
        )
        assertEquals(DateTime(2026, 3, 2, 9, 0), next("FREQ=WEEKLY;BYDAY=MO,WE,FR", feb27))
    }

    @Test
    fun agreesWhenARuleHasNoFurtherOccurrence() {
        val feb27 = DateTime(2026, 2, 27, 9, 0)

        assertNull(next("FREQ=DAILY;COUNT=1", feb27))
        assertNull(next("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=30", feb27))
    }

    private fun next(rrule: String, start: DateTime) =
        Recur.parse(rrule).nextOccurrence(start, hasTime = true)
}
