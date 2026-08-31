package org.tasks.notifications

import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.tasks.DatabaseTest
import org.tasks.data.dao.AlarmDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.NotificationScheduler.Companion.HELD
import org.tasks.notifications.NotificationScheduler.Companion.MAX_HOLD
import org.tasks.notifications.NotificationScheduler.Companion.MAX_WAIT
import org.tasks.notifications.NotificationScheduler.Companion.MIN_WAIT
import org.tasks.preferences.AppPreferences
import org.tasks.reminders.Random
import org.tasks.time.DateTimeUtils2
import org.tasks.time.endOfMinute
import org.tasks.time.startOfDay
import org.tasks.time.startOfMinute
import org.tasks.time.withMillisOfDay

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSchedulerTest : DatabaseTest() {
    private val alarmDao = db.alarmDao()
    private val taskDao = db.taskDao()

    private val preferences: AppPreferences = mock {
        onBlocking { isCurrentlyQuietHours() } doReturn false
        onBlocking { defaultDueTime() } doAnswer { defaultDueTime }
    }

    private var defaultDueTime = 0

    private val alarmService: () -> AlarmService = {
        AlarmService(
            alarmDao = alarmDao,
            taskDao = taskDao,
            dirtyDao = db.dirtyDao(),
            refreshBroadcaster = mock(),
            notifier = mock(),
            alarmCalculator = AlarmCalculator(Random()),
            preferences = preferences,
        )
    }

    private val fired = mutableListOf<Notification>()

    private val undeliverable = mutableSetOf<Long>()

    private var failWith: Throwable? = null

    private val trigger: suspend (List<Notification>) -> Collection<Long> = { notifications ->
        failWith?.let { throw it }
        val delivered = notifications.filterNot { it.taskId in undeliverable }
        fired.addAll(delivered)
        delivered.forEach { taskDao.setLastNotified(it.taskId, it.timestamp.endOfMinute()) }
        delivered.map { it.taskId }
    }

    private val scheduler = NotificationScheduler(alarmService, trigger)

    private var now = 0L

    @After
    fun restoreClock() {
        DateTimeUtils2.setCurrentMillisSystem()
    }

    @Test
    fun overdueAlarmFiresOnlyOnce() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))

        repeat(5) {
            scheduler.scanOnce()
            setTime(now + SCAN_INTERVAL)
        }

        assertEquals(1, fired.size)
    }

    @Test
    fun repeatingAlarmFiresAtIntervalSpacing() = runTest {
        setTime(MINUTE_ALIGNED)
        val start = now
        val task = createTask()
        alarmDao.insert(
            Alarm(
                task = task.id,
                time = start,
                type = TYPE_DATE_TIME,
                repeat = 2,
                interval = INTERVAL,
            )
        )

        repeat(15) {
            scheduler.scanOnce()
            setTime(now + SCAN_INTERVAL)
        }

        assertEquals(
            listOf(start, start + INTERVAL, start + 2 * INTERVAL),
            fired.map { it.timestamp },
        )
    }

    @Test
    fun nothingFiresDuringQuietHours() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        setQuietHours(true, until = now + 4 * SCAN_INTERVAL)

        repeat(3) {
            scheduler.scanOnce()
            setTime(now + SCAN_INTERVAL)
        }

        assertTrue(fired.isEmpty())
    }

    @Test
    fun firesOnceQuietHoursAreOver() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        setQuietHours(true, until = now + SCAN_INTERVAL)

        scheduler.scanOnce()
        assertTrue(fired.isEmpty())

        setTime(now + SCAN_INTERVAL)
        setQuietHours(false)
        scheduler.scanOnce()

        assertEquals(1, fired.size)
    }

    @Test
    fun snoozeIsWaitedOutRatherThanRefired() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))

        scheduler.scanOnce()
        assertEquals(1, fired.size)

        alarmDao.insert(
            Alarm(task = task.id, time = now + 3 * SCAN_INTERVAL, type = TYPE_SNOOZE)
        )
        repeat(2) {
            setTime(now + SCAN_INTERVAL)
            scheduler.scanOnce()
        }
        assertEquals(1, fired.size)

        setTime(now + 2 * SCAN_INTERVAL)
        scheduler.scanOnce()

        assertEquals(2, fired.size)
        assertEquals(TYPE_SNOOZE, fired.last().type)
    }

    @Test
    fun anUndeliveredSnoozeIsNotCleared() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))

        scheduler.scanOnce()
        assertEquals(1, fired.size)

        alarmDao.insert(Alarm(task = task.id, time = now + SCAN_INTERVAL, type = TYPE_SNOOZE))
        undeliverable.add(task.id)
        setTime(now + SCAN_INTERVAL)
        scheduler.scanOnce()

        assertEquals(1, fired.size)

        undeliverable.clear()
        setTime(now + SCAN_INTERVAL)
        scheduler.scanOnce()

        assertEquals(2, fired.size)
        assertEquals(TYPE_SNOOZE, fired.last().type)
    }

    @Test
    fun changedDefaultReminderTimeAppliesWithoutRestart() = runTest {
        val today = MINUTE_ALIGNED.startOfDay()
        setTime(today.withMillisOfDay(NINE_THIRTY_AM))
        defaultDueTime = SIX_PM

        val task = createTask(dueDate = today)
        alarmDao.insert(Alarm.whenDue(task.id))

        scheduler.scanOnce()
        assertTrue(fired.isEmpty())

        defaultDueTime = NINE_AM
        scheduler.scanOnce()

        assertEquals(1, fired.size)
    }

    @Test
    fun anAlarmThatWasNotDeliveredStillCostsAFullInterval() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        undeliverable.add(task.id)

        assertEquals(MAX_WAIT, scheduler.scanOnce())
        assertTrue(fired.isEmpty())
    }

    @Test
    fun waitsUntilTheNextAlarmWhenOneIsComing() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = now + 10 * SCAN_INTERVAL, type = TYPE_DATE_TIME)
        )

        assertEquals(MAX_WAIT, scheduler.scanOnce())

        setTime(now + 10 * SCAN_INTERVAL - 5_000L)
        assertEquals(5_000L, scheduler.scanOnce())
    }

    @Test
    fun nothingScheduledStillRechecks() = runTest {
        setTime(MINUTE_ALIGNED)
        createTask()

        assertEquals(MAX_WAIT, scheduler.scanOnce())
    }

    @Test
    fun aScanThatBlowsUpDoesNotKillTheLoop() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        failWith = UnsatisfiedLinkError("no UserNotifications framework")

        assertEquals(MAX_WAIT, scheduler.scanOnce())

        failWith = null
        scheduler.scanOnce()

        assertEquals(1, fired.size)
    }

    @Test
    fun startingTwiceDoesNotRunTwoLoops() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val scheduler = emptyScheduler { scans++ }

        scheduler.start(backgroundScope)
        scheduler.start(backgroundScope)
        runCurrent()

        assertEquals(1, scans)

        scheduler.signal()
        advanceTimeBy(MIN_WAIT)
        runCurrent()
        assertEquals(2, scans)

        scheduler.stop()
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertEquals(2, scans)
    }

    @Test
    fun startingAgainDoesNotDropAReconcileThatIsStillUnwinding() = runTest(StandardTestDispatcher()) {
        val scheduler = emptyScheduler { }
        var reconciles = 0

        val blockers = List(2) { CompletableDeferred<Unit>() }
        val reconcile: suspend () -> Unit = {
            val blocker = blockers.getOrNull(reconciles)
            reconciles++

            blocker?.let { withContext(NonCancellable) { it.await() } }
        }
        val firstScope = CoroutineScope(coroutineContext + Job())

        scheduler.start(firstScope, reconcile = reconcile)
        runCurrent()
        assertEquals(1, reconciles)

        firstScope.cancel()
        runCurrent()

        scheduler.start(backgroundScope, reconcile = reconcile)
        runCurrent()

        blockers[1].complete(Unit)
        runCurrent()

        val stopping = launch { scheduler.stop() }
        runCurrent()
        assertTrue("stop returned without waiting for the orphaned reconcile", stopping.isActive)

        blockers[0].complete(Unit)
        runCurrent()
        assertTrue(stopping.isCompleted)
    }

    @Test
    fun aReconcileThatWasCancelledPartWayIsRunAgainForTheNextSession() = runTest(StandardTestDispatcher()) {
        val scheduler = emptyScheduler { }
        var reconciles = 0
        val blockers = List(2) { CompletableDeferred<Unit>() }
        val reconcile: suspend () -> Unit = {
            val blocker = blockers.getOrNull(reconciles)
            reconciles++
            blocker?.let { withContext(NonCancellable) { it.await() } }
        }
        val firstScope = CoroutineScope(coroutineContext + Job())

        scheduler.start(firstScope, reconcile = reconcile)
        runCurrent()
        firstScope.cancel()
        runCurrent()

        scheduler.start(backgroundScope, reconcile = reconcile)
        runCurrent()

        blockers[0].complete(Unit)
        runCurrent()
        assertEquals(2, reconciles)

        blockers[1].complete(Unit)
        scheduler.stop()
    }

    @Test
    fun aSecondReconcileWaitsForTheOneItReplacedToFinish() = runTest(StandardTestDispatcher()) {
        val scheduler = emptyScheduler { }
        var started = 0
        var running = 0
        var overlapped = false
        val blockers = List(2) { CompletableDeferred<Unit>() }
        val reconcile: suspend () -> Unit = {
            val blocker = blockers.getOrNull(started)
            started++
            running++
            overlapped = overlapped || running > 1
            try {
                blocker?.let { withContext(NonCancellable) { it.await() } }
            } finally {
                running--
            }
        }
        val firstScope = CoroutineScope(coroutineContext + Job())

        scheduler.start(firstScope, reconcile = reconcile)
        runCurrent()
        firstScope.cancel()
        runCurrent()

        scheduler.start(backgroundScope, reconcile = reconcile)
        runCurrent()

        assertEquals(1, started)

        blockers[0].complete(Unit)
        runCurrent()

        assertEquals(2, started)
        assertTrue("two reconciles corrected the same table at once", !overlapped)

        blockers[1].complete(Unit)
        scheduler.stop()
    }

    @Test
    fun stoppingWaitsForAScanADisposedCompositionLeftBehind() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val blockers = List(2) { CompletableDeferred<Unit>() }
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },
            trigger = {
                val blocker = blockers.getOrNull(scans)
                scans++
                blocker?.let { withContext(NonCancellable) { it.await() } }
                emptyList()
            },
        )
        val firstScope = CoroutineScope(coroutineContext + Job())

        scheduler.start(firstScope)
        runCurrent()
        assertEquals(1, scans)

        firstScope.cancel()
        runCurrent()

        scheduler.start(backgroundScope)
        runCurrent()
        assertEquals(2, scans)

        blockers[1].complete(Unit)
        runCurrent()

        val stopping = launch { scheduler.stop() }
        runCurrent()
        assertTrue("stop returned without waiting for the orphaned scan", stopping.isActive)

        blockers[0].complete(Unit)
        runCurrent()
        assertTrue(stopping.isCompleted)
    }

    @Test
    fun aScanThatOutlivesTheStopBudgetIsStillWaitedForByTheNextStop() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val blocker = CompletableDeferred<Unit>()
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },
            trigger = {
                scans++
                withContext(NonCancellable) { blocker.await() }
                emptyList()
            },
        )

        scheduler.start(backgroundScope)
        runCurrent()
        assertEquals(1, scans)

        val gaveUp = launch { scheduler.stop() }
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertTrue("stop waited past its budget", gaveUp.isCompleted)

        val second = launch { scheduler.stop() }
        runCurrent()
        assertTrue("the scan the first stop gave up on was forgotten", second.isActive)

        blocker.complete(Unit)
        runCurrent()
        assertTrue(second.isCompleted)
    }

    @Test
    fun startingAgainAfterStoppingResumesScanning() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val scheduler = emptyScheduler { scans++ }

        scheduler.start(backgroundScope)
        runCurrent()
        scheduler.stop()
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertEquals(1, scans)

        scheduler.start(backgroundScope)
        runCurrent()

        assertEquals(2, scans)
        scheduler.stop()
    }

    @Test
    fun waitsForTheNextAlarmButNoLongerThanTheCap() {
        val next = 1_000_000L
        assertEquals(MAX_WAIT, NotificationScheduler.waitFor(next, now = next - 10 * MAX_WAIT))
        assertEquals(30_000L, NotificationScheduler.waitFor(next, now = next - 30_000L))

        assertEquals(MAX_WAIT, NotificationScheduler.waitFor(0, now = next))

        assertEquals(MIN_WAIT, NotificationScheduler.waitFor(next, now = next + MAX_WAIT))
    }

    @Test
    fun loopRescansOnItsOwnAndWhenSignalled() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val scheduler = emptyScheduler { scans++ }
        scheduler.start(backgroundScope)

        runCurrent()
        assertEquals(1, scans)

        advanceTimeBy(MAX_WAIT)
        runCurrent()
        assertEquals(2, scans)

        scheduler.signal()
        runCurrent()
        assertEquals(2, scans)
        advanceTimeBy(MIN_WAIT)
        runCurrent()
        assertEquals(3, scans)

        scheduler.stop()
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertEquals(3, scans)
    }

    @Test
    fun signalsCannotDriveTheLoopFasterThanTheFloor() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val scheduler = emptyScheduler { scans++ }
        scheduler.start(backgroundScope)
        runCurrent()
        assertEquals(1, scans)

        repeat(100) {
            scheduler.signal()
            runCurrent()
        }
        assertEquals(1, scans)

        advanceTimeBy(MIN_WAIT)
        runCurrent()
        assertEquals(2, scans)

        scheduler.stop()
    }

    @Test
    fun theFloorDoesNotDelayTheOrdinaryTick() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val scheduler = emptyScheduler { scans++ }
        scheduler.start(backgroundScope)
        runCurrent()

        advanceTimeBy(MAX_WAIT - 1)
        runCurrent()
        assertEquals(1, scans)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, scans)

        scheduler.stop()
    }

    @Test
    fun stopWaitsForTheScanInFlight() = runTest(StandardTestDispatcher()) {
        var finished = false
        val scheduler = NotificationScheduler(
            alarmService = {
                AlarmService(
                    alarmDao = mock<AlarmDao> { onBlocking { getActiveAlarms() } doReturn emptyList() },
                    taskDao = taskDao,
                    dirtyDao = db.dirtyDao(),
                    refreshBroadcaster = mock(),
                    notifier = mock(),
                    alarmCalculator = AlarmCalculator(Random()),
                    preferences = preferences,
                )
            },
            trigger = {
                try {
                    delay(MAX_WAIT)
                } finally {
                    finished = true
                }
                emptyList()
            },
        )
        scheduler.start(backgroundScope)
        runCurrent()

        scheduler.stop()

        assertTrue(finished)
    }

    @Test
    fun theReconcileRunsWithoutHoldingUpTheFirstScan() = runTest(StandardTestDispatcher()) {
        var scans = 0
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val scheduler = emptyScheduler { scans++ }

        scheduler.start(backgroundScope) {
            started.complete(Unit)
            release.await()
        }
        runCurrent()

        assertTrue(started.isCompleted)
        assertEquals(1, scans)

        release.complete(Unit)
        scheduler.stop()
    }

    @Test
    fun stoppingCancelsAndWaitsForTheReconcileToo() = runTest(StandardTestDispatcher()) {
        val scheduler = emptyScheduler { }
        var unwound = false
        val started = CompletableDeferred<Unit>()

        scheduler.start(backgroundScope) {
            try {
                started.complete(Unit)

                delay(10 * MAX_WAIT)
            } finally {
                unwound = true
            }
        }
        runCurrent()
        assertTrue(started.isCompleted)
        assertTrue(!unwound)

        scheduler.stop()

        assertTrue(unwound)
    }

    @Test
    fun stoppingWaitsForTheScanAsWellAsTheReconcile() = runTest(StandardTestDispatcher()) {
        var scanUnwound = false
        var reconcileUnwound = false
        val scanning = CompletableDeferred<Unit>()
        val reconciling = CompletableDeferred<Unit>()
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },

            trigger = {
                try {
                    scanning.complete(Unit)
                    delay(10 * MAX_WAIT)
                    emptyList()
                } finally {
                    scanUnwound = true
                }
            },
        )

        scheduler.start(backgroundScope) {
            try {
                reconciling.complete(Unit)
                delay(10 * MAX_WAIT)
            } finally {
                reconcileUnwound = true
            }
        }
        runCurrent()
        assertTrue(scanning.isCompleted)
        assertTrue(reconciling.isCompleted)

        scheduler.stop()

        assertTrue(scanUnwound)
        assertTrue(reconcileUnwound)
    }

    @Test
    fun aHeldScanNeverPostsAnything() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        var held = Hold.UNTIL_SIGNALLED
        val scheduler = NotificationScheduler(alarmService, trigger, hold = { held })

        assertEquals(HELD, scheduler.scanOnce())

        held = Hold.UNTIL_NEXT_ALARM
        assertEquals(HELD, scheduler.scanOnce())

        assertTrue(fired.isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)
    }

    @Test
    fun aHeldScanWaitsOutTheNextFutureAlarmRatherThanTheCap() = runTest {
        setTime(MINUTE_ALIGNED)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - 1, type = TYPE_DATE_TIME))
        val later = createTask()
        val future = now + 10 * MAX_WAIT
        alarmDao.insert(Alarm(task = later.id, time = future, type = TYPE_DATE_TIME))
        val scheduler = NotificationScheduler(alarmService, trigger, hold = { Hold.UNTIL_NEXT_ALARM })

        assertEquals(future - now, scheduler.scanOnce())
        assertTrue(fired.isEmpty())
    }

    @Test
    fun anAlarmThatPassedWhileHeldIsSweptUpOnceTheHoldLifts() = runTest {
        setTime(MINUTE_ALIGNED)
        val missed = createTask()
        alarmDao.insert(Alarm(task = missed.id, time = now - 1, type = TYPE_DATE_TIME))
        val later = createTask()
        val future = now + 10 * MAX_WAIT
        alarmDao.insert(Alarm(task = later.id, time = future, type = TYPE_DATE_TIME))
        var held = Hold.UNTIL_NEXT_ALARM
        val scheduler = NotificationScheduler(alarmService, trigger, hold = { held })

        scheduler.scanOnce()
        assertTrue(fired.isEmpty())

        setTime(future)
        held = Hold.NONE
        scheduler.scanOnce()

        assertEquals(setOf(missed.id, later.id), fired.map { it.taskId }.toSet())
    }

    @Test
    fun aLoopHeldUntilSignalledDoesNotRescanOnItsOwn() = runTest(StandardTestDispatcher()) {
        var scans = 0
        var held = Hold.UNTIL_SIGNALLED
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },
            trigger = { scans++; emptyList() },
            hold = { held },
        )

        scheduler.start(backgroundScope)
        runCurrent()
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertEquals(0, scans)

        held = Hold.NONE
        scheduler.signal()
        runCurrent()
        assertEquals(1, scans)

        scheduler.stop()
    }

    @Test
    fun aLoopHeldWithNothingElseDueWaitsRatherThanPolling() = runTest(StandardTestDispatcher()) {
        var scans = 0
        var held = Hold.UNTIL_NEXT_ALARM
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },
            trigger = { scans++; emptyList() },
            hold = { held },
        )

        scheduler.start(backgroundScope)
        runCurrent()
        advanceTimeBy(10 * MAX_WAIT)
        runCurrent()
        assertEquals(0, scans)

        held = Hold.NONE
        scheduler.signal()
        runCurrent()
        assertEquals(1, scans)

        scheduler.stop()
    }

    @Test
    fun holdsUntilTheNextAlarmOrUntilSignalledWhenThereIsNone() {
        val next = 1_000_000L

        assertEquals(10 * MAX_WAIT, NotificationScheduler.holdUntil(next, now = next - 10 * MAX_WAIT))
        assertEquals(MAX_HOLD, NotificationScheduler.holdUntil(next, now = next - 10 * MAX_HOLD))
        assertEquals(MIN_WAIT, NotificationScheduler.holdUntil(next, now = next + MAX_WAIT))
        assertEquals(HELD, NotificationScheduler.holdUntil(0, now = next))
    }

    @Test
    fun aLoopHeldUntilSignalledRechecksAtTheCap() = runTest(StandardTestDispatcher()) {
        assertRechecksAtTheCap(Hold.UNTIL_SIGNALLED)
    }

    @Test
    fun aLoopHeldWithNothingElseDueRechecksAtTheCap() = runTest(StandardTestDispatcher()) {
        assertRechecksAtTheCap(Hold.UNTIL_NEXT_ALARM)
    }

    private suspend fun TestScope.assertRechecksAtTheCap(held: Hold) {
        var holds = 0
        val scheduler = NotificationScheduler(
            alarmService = { emptyAlarmService() },
            trigger = { emptyList() },
            hold = { holds++; held },
        )

        scheduler.start(backgroundScope)
        runCurrent()
        assertEquals(1, holds)

        advanceTimeBy(MAX_HOLD - 1)
        runCurrent()
        assertEquals(1, holds)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, holds)

        scheduler.stop()
    }

    private fun emptyScheduler(onScan: () -> Unit) = NotificationScheduler(
        alarmService = { emptyAlarmService() },
        trigger = {
            onScan()
            emptyList()
        },
    )

    private fun emptyAlarmService() = AlarmService(
        alarmDao = mock<AlarmDao> { onBlocking { getActiveAlarms() } doReturn emptyList() },
        taskDao = taskDao,
        dirtyDao = db.dirtyDao(),
        refreshBroadcaster = mock(),
        notifier = mock(),
        alarmCalculator = AlarmCalculator(Random()),
        preferences = preferences,
    )

    private suspend fun createTask(dueDate: Long = 0): Task =
        Task(dueDate = dueDate).also { taskDao.createNew(it) }

    private fun setTime(millis: Long) {
        now = millis
        DateTimeUtils2.setCurrentMillisFixed(millis)
    }

    private fun setQuietHours(quiet: Boolean, until: Long = 0) {
        preferences.stub {
            onBlocking { isCurrentlyQuietHours() } doReturn quiet
            onBlocking { adjustForQuietHours(any()) } doReturn until
        }
    }

    companion object {
        private val MINUTE_ALIGNED = 1_700_000_000_000L.startOfMinute()
        private const val SCAN_INTERVAL = MAX_WAIT
        private const val INTERVAL = 5 * MAX_WAIT
        private const val NINE_AM = 9 * 60 * 60 * 1000
        private const val NINE_THIRTY_AM = NINE_AM + 30 * 60 * 1000
        private const val SIX_PM = 18 * 60 * 60 * 1000
    }
}
