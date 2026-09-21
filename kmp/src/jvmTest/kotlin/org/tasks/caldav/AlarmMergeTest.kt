package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Alarm

class AlarmMergeTest {
    @Test
    fun allEmpty() {
        assertEquals(emptySet<Alarm>(), merge(base = listOf(), local = listOf(), remote = listOf()))
    }

    @Test
    fun unchangedKept() {
        assertEquals(
            setOf(dateTime(1000)),
            merge(base = listOf(dateTime(1000)), local = listOf(dateTime(1000)), remote = listOf(dateTime(1000)))
        )
    }

    // remote has a reminder but cache doesn't -> add it
    @Test
    fun remoteAddedIsKept() {
        assertEquals(
            setOf(dateTime(1000), dateTime(2000)),
            merge(
                base = listOf(dateTime(1000)),
                local = listOf(dateTime(1000)),
                remote = listOf(dateTime(1000), dateTime(2000)),
            )
        )
    }

    // local has a reminder but cache doesn't -> add it
    @Test
    fun localAddedIsKept() {
        assertEquals(
            setOf(dateTime(1000), dateTime(2000)),
            merge(
                base = listOf(dateTime(1000)),
                local = listOf(dateTime(1000), dateTime(2000)),
                remote = listOf(dateTime(1000)),
            )
        )
    }

    // cache has a reminder but remote doesn't -> remove it locally
    @Test
    fun remoteDeletedIsRemoved() {
        assertEquals(
            setOf(dateTime(1000)),
            merge(
                base = listOf(dateTime(1000), dateTime(2000)),
                local = listOf(dateTime(1000), dateTime(2000)),
                remote = listOf(dateTime(1000)),
            )
        )
    }

    // cache has a reminder but local doesn't -> stays removed
    @Test
    fun localDeletedIsRemoved() {
        assertEquals(
            setOf(dateTime(1000)),
            merge(
                base = listOf(dateTime(1000), dateTime(2000)),
                local = listOf(dateTime(1000)),
                remote = listOf(dateTime(1000), dateTime(2000)),
            )
        )
    }

    @Test
    fun bothSidesDeleteSame() {
        assertEquals(
            setOf(dateTime(1000)),
            merge(
                base = listOf(dateTime(1000), dateTime(2000)),
                local = listOf(dateTime(1000)),
                remote = listOf(dateTime(1000)),
            )
        )
    }

    @Test
    fun bothSidesAddSameIsDeduped() {
        assertEquals(
            setOf(dateTime(2000)),
            merge(base = listOf(), local = listOf(dateTime(2000)), remote = listOf(dateTime(2000)))
        )
    }

    @Test
    fun conflictingAddsKeepBoth() {
        assertEquals(
            setOf(whenStarted, whenDue),
            merge(base = listOf(), local = listOf(whenStarted), remote = listOf(whenDue))
        )
    }

    // random reminders are local-only and unbounded; they must always survive a merge
    @Test
    fun randomRemindersPreserved() {
        assertEquals(
            setOf(whenDue, random(3600000), random(86400000)),
            merge(
                base = listOf(whenDue),
                local = listOf(whenDue, random(3600000), random(86400000)),
                remote = listOf(whenDue),
            )
        )
    }

    @Test
    fun randomPreservedWhileReminderChanges() {
        assertEquals(
            setOf(dateTime(2000), random(3600000)),
            merge(
                base = listOf(dateTime(1000)),
                local = listOf(dateTime(1000), random(3600000)),
                remote = listOf(dateTime(2000)),
            )
        )
    }

    @Test
    fun snoozeAddedRemotely() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(), local = listOf(), remote = listOf(snooze(5000)))
        )
    }

    // snooze expired locally (removed) while another device set a new snooze
    @Test
    fun remoteSnoozeAppliedAfterLocalSnoozeExpired() {
        assertEquals(
            setOf(whenDue, snooze(5000)),
            merge(
                base = listOf(whenDue, snooze(1000)),
                local = listOf(whenDue),
                remote = listOf(whenDue, snooze(5000)),
            )
        )
    }

    @Test
    fun expiredLocalSnoozeStaysGoneWhenRemoteUnchanged() {
        assertEquals(
            emptySet<Alarm>(),
            merge(base = listOf(snooze(1000)), local = listOf(), remote = listOf(snooze(1000)))
        )
    }

    @Test
    fun remoteSnoozeWinsWhenLocalUnchanged() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(snooze(1000)), local = listOf(snooze(1000)), remote = listOf(snooze(5000)))
        )
    }

    @Test
    fun localSnoozeWinsWhenRemoteUnchanged() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(snooze(1000)), local = listOf(snooze(5000)), remote = listOf(snooze(1000)))
        )
    }

    // snoozed on two devices at once: latest snooze wins, only one snooze survives
    @Test
    fun concurrentSnoozeLatestWinsWithBase() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(snooze(1000)), local = listOf(snooze(3000)), remote = listOf(snooze(5000)))
        )
    }

    @Test
    fun concurrentSnoozeLatestWinsNoBase() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(), local = listOf(snooze(3000)), remote = listOf(snooze(5000)))
        )
    }

    @Test
    fun concurrentSnoozeLocalLaterWins() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(), local = listOf(snooze(5000)), remote = listOf(snooze(3000)))
        )
    }

    @Test
    fun identicalConcurrentSnoozeIsSingle() {
        assertEquals(
            setOf(snooze(5000)),
            merge(base = listOf(), local = listOf(snooze(5000)), remote = listOf(snooze(5000)))
        )
    }

    @Test
    fun remindersSnoozeAndRandomTogether() {
        assertEquals(
            setOf(whenDue, dateTime(2000), snooze(5000), random(3600000)),
            merge(
                base = listOf(whenDue, snooze(1000)),
                local = listOf(whenDue, snooze(3000), random(3600000)),
                remote = listOf(whenDue, dateTime(2000), snooze(5000)),
            )
        )
    }

    @Test
    fun flattenedBasePreservesRichLocalAlarm() {
        assertEquals(
            setOf(repeating),
            merge(base = listOf(flattened), local = listOf(repeating), remote = listOf(flattened)),
        )
    }

    @Test
    fun richBaseClobbersRepeatOnFlattenedRoundTrip() {
        assertEquals(
            setOf(flattened),
            merge(base = listOf(repeating), local = listOf(repeating), remote = listOf(flattened)),
        )
    }

    // a remote device adds another reminder; the flattened repeating alarm must still survive
    @Test
    fun remoteAddedReminderPreservesFlattenedLocal() {
        assertEquals(
            setOf(repeating, whenDue),
            merge(
                base = listOf(flattened),
                local = listOf(repeating),
                remote = listOf(flattened, whenDue),
            ),
        )
    }

    // a remote device removes a different reminder; the flattened repeating alarm must still survive
    @Test
    fun remoteRemovedOtherReminderPreservesFlattenedLocal() {
        assertEquals(
            setOf(repeating),
            merge(
                base = listOf(flattened, whenDue),
                local = listOf(repeating, whenDue),
                remote = listOf(flattened),
            ),
        )
    }

    private fun merge(base: List<Alarm>, local: List<Alarm>, remote: List<Alarm>) =
        mergeReminders(base = base, local = local, remote = remote)

    private val whenDue = Alarm(type = Alarm.TYPE_REL_END)
    private val whenStarted = Alarm(type = Alarm.TYPE_REL_START)
    private fun dateTime(time: Long) = Alarm(type = Alarm.TYPE_DATE_TIME, time = time)
    private fun random(time: Long) = Alarm(type = Alarm.TYPE_RANDOM, time = time)
    private fun snooze(time: Long) = Alarm(type = Alarm.TYPE_SNOOZE, time = time)
    private val repeating = Alarm(type = Alarm.TYPE_REL_START, time = 1000, repeat = 15, interval = 2000)
    private val flattened = Alarm(type = Alarm.TYPE_REL_START, time = 1000)
}
