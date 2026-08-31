package org.tasks.notifications

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.DatabaseTest
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2
import org.tasks.time.endOfMinute
import org.tasks.time.startOfMinute

class DesktopNotifierTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val notificationDao = db.notificationDao()
    private val backend = RecordingNotifications()

    private val alarmDao = db.alarmDao()
    private val refreshBroadcaster = mock<RefreshBroadcaster>()

    private val built = mutableListOf<RecordingNotifications>()
    private var nextBackend: () -> RecordingNotifications? = { backend }

    private var screenCleared = false

    private var elapsed = 0L

    private var platformIdsAreOurs = true

    private val notifier = DesktopNotifier(
        taskDao = taskDao,
        notificationDao = notificationDao,
        alarmDao = alarmDao,
        refreshBroadcaster = refreshBroadcaster,
        signalScheduler = {},
        recordScreenCleared = { screenCleared = true },
        takeScreenCleared = { screenCleared.also { screenCleared = false } },
        claimPlatformIds = { platformIdsAreOurs },
        elapsedRealtime = { elapsed },
        createBackend = { nextBackend()?.also { built.add(it) } },
    )

    @After
    fun resetClock() {
        DateTimeUtils2.setCurrentMillisSystem()
    }

    @Test
    fun postsNotificationAndRecordsIt() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants", notes = "the big one too"))

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(1, backend.shown.size)
        val shown = backend.shown.single()
        assertEquals(task.id, shown.taskId)
        assertEquals("Water the plants", shown.title)
        assertEquals("the big one too", shown.body)

        assertEquals(NOW.endOfMinute(), taskDao.fetch(task.id)!!.reminderLast)
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun offersCompleteOnlyForATaskTheAccountCanWriteTo() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val editable = createTask(Task(title = "editable"))
        val readOnly = createTask(Task(title = "read only", readOnly = true))

        notifier.triggerNotifications(
            listOf(notification(editable.id), notification(readOnly.id))
        )

        assertEquals(
            listOf(
                NotificationAction.OPEN,
                NotificationAction.COMPLETE,
                NotificationAction.SNOOZE,
            ),
            backend.shown.first { it.taskId == editable.id }.actions,
        )

        assertEquals(
            listOf(NotificationAction.OPEN, NotificationAction.SNOOZE),
            backend.shown.first { it.taskId == readOnly.id }.actions,
        )
    }

    @Test
    fun aBackendThatHearsNothingBackIsNotOfferedActions() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.opens = false
        backend.actions = false

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(emptyList<NotificationAction>(), backend.shown.single().actions)
    }

    @Test
    fun dropsCompletedAndDeletedTasks() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val completed = createTask(Task(title = "done", completionDate = NOW))
        val deleted = createTask(Task(title = "gone", deletionDate = NOW))

        notifier.triggerNotifications(
            listOf(notification(completed.id), notification(deleted.id))
        )

        assertTrue(backend.shown.isEmpty())
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun cancelClearsTheNotificationAndTellsTheBackend() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertTrue(notificationDao.getAll().isEmpty())
        assertEquals(listOf(listOf(task.id)), backend.dismissed)
    }

    @Test
    fun cancellingATaskWithNoNotificationDoesNothing() = runTest {
        val task = createTask(Task(title = "Water the plants"))

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertTrue(backend.dismissed.isEmpty())
        assertTrue("a backend was built to dismiss nothing", built.isEmpty())
        verify(refreshBroadcaster, never()).broadcastRefresh()
    }

    @Test
    fun remindersAreHeldRatherThanLostWhilePermissionIsUndecided() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.permission = NotificationPermission.NOT_DETERMINED

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(backend.shown.isEmpty())

        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)
        assertTrue(notificationDao.getAll().isEmpty())

        backend.permission = NotificationPermission.GRANTED
        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(1, backend.shown.size)
        assertEquals(NOW.endOfMinute(), taskDao.fetch(task.id)!!.reminderLast)
    }

    @Test
    fun permissionIsOnlyRequestedWhenThereIsAReminderToDeliver() = runTest {
        val task = createTask(Task(title = "Water the plants"))
        backend.permission = NotificationPermission.NOT_DETERMINED

        notifier.requestPermissionIfNeeded()
        assertEquals(0, backend.permissionRequests)

        alarmDao.insert(Alarm(task = task.id, time = NOW, type = Alarm.TYPE_DATE_TIME))
        notifier.requestPermissionIfNeeded()

        assertEquals(1, backend.permissionRequests)

        repeat(5) { notifier.requestPermissionIfNeeded() }

        assertEquals(1, backend.permissionRequests)
    }

    @Test
    fun alreadyAnsweredPermissionIsNotAskedAgain() = runTest {
        val task = createTask(Task(title = "Water the plants"))
        alarmDao.insert(Alarm(task = task.id, time = NOW, type = Alarm.TYPE_DATE_TIME))

        backend.permission = NotificationPermission.DENIED

        notifier.requestPermissionIfNeeded()

        assertEquals(0, backend.permissionRequests)
    }

    @Test
    fun aReminderThatNeverGotOutIsNotRecordedAsDelivered() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.posts = false

        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(delivered.isEmpty())

        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)

        assertTrue(notificationDao.getAll().isEmpty())

        backend.posts = true
        assertEquals(listOf(task.id), notifier.triggerNotifications(listOf(notification(task.id))))
        assertEquals(NOW.endOfMinute(), taskDao.fetch(task.id)!!.reminderLast)
    }

    @Test
    fun aScanThatPostedNothingDoesNotBroadcast() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.posts = false

        notifier.triggerNotifications(listOf(notification(task.id)))

        verify(refreshBroadcaster, never()).broadcastRefresh()

        backend.posts = true
        notifier.triggerNotifications(listOf(notification(task.id)))

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun aPermissionCheckThatBlowsUpIsNotRethrown() = runTest {
        val task = createTask(Task(title = "Water the plants"))
        alarmDao.insert(Alarm(task = task.id, time = NOW, type = Alarm.TYPE_DATE_TIME))

        backend.throwOnPermission = true

        notifier.requestPermissionIfNeeded()

        backend.throwOnPermission = false
        backend.permission = NotificationPermission.NOT_DETERMINED
        notifier.requestPermissionIfNeeded()

        assertEquals(1, backend.permissionRequests)
    }

    @Test
    fun aWorkingBackendIsNeverRebuilt() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))

        notifier.triggerNotifications(listOf(notification(task.id)))
        DateTimeUtils2.setCurrentMillisFixed(NOW + 60 * 60_000L)
        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(listOf(backend), built)
    }

    @Test
    fun remindersAreHeldRatherThanLostWhilePermissionIsRefused() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.permission = NotificationPermission.DENIED

        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(delivered.isEmpty())
        assertTrue(backend.shown.isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)
        assertTrue(notificationDao.getAll().isEmpty())

        backend.permission = NotificationPermission.GRANTED
        assertEquals(listOf(task.id), notifier.triggerNotifications(listOf(notification(task.id))))
    }

    @Test
    fun aPermissionCheckThatBlowsUpMidScanCostsNothing() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.throwOnPermission = true

        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(delivered.isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)
        assertTrue(notificationDao.getAll().isEmpty())

        backend.throwOnPermission = false
        assertEquals(listOf(task.id), notifier.triggerNotifications(listOf(notification(task.id))))
    }

    @Test
    fun aDismissThatBlowsUpDoesNotFailTheCancel() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))
        backend.throwOnDismiss = true

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun aSuccessfulDismissClearsTheRow() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertEquals(listOf(listOf(task.id)), backend.dismissed)
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun startupNeverRepostsAnything() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        backend.deliveredIds = DeliveredQuery.Known(setOf(task.id))

        notifier.reconcileNotifications()

        assertTrue(backend.shown.isEmpty())

        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun survivingNotificationsAreRepostedWhereTheirActionsDiedWithTheProcess() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val stillUp = createTask(Task(title = "Water the plants"))
        val cleared = createTask(Task(title = "swiped away overnight"))
        notificationDao.insertAll(
            listOf(notification(stillUp.id), notification(cleared.id))
        )
        backend.deliveredIds = DeliveredQuery.Known(setOf(stillUp.id))
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        val shown = backend.shown.single()
        assertEquals(stillUp.id, shown.taskId)
        assertTrue(shown.actions.contains(NotificationAction.COMPLETE))

        assertEquals(Alert.SUPPRESSED, shown.alert)

        assertEquals(listOf(stillUp.id), notificationDao.getAll())
    }

    @Test
    fun survivingNotificationsAreLeftAloneWhereTheirActionsStillWork() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        backend.deliveredIds = DeliveredQuery.Known(setOf(task.id))
        backend.actionsSurvive = true

        notifier.reconcileNotifications()

        assertTrue(backend.shown.isEmpty())
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun nothingIsRepostedWhenNothingSurvived() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        backend.deliveredIds = DeliveredQuery.Known(emptySet())
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        assertTrue(backend.shown.isEmpty())
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun rowsForNotificationsClearedWhileClosedAreDropped() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val stillUp = createTask(Task(title = "Water the plants"))
        val cleared = createTask(Task(title = "swiped away overnight"))
        val gone = createTask(Task(title = "already dealt with", completionDate = NOW))
        notificationDao.insertAll(
            listOf(notification(stillUp.id), notification(cleared.id), notification(gone.id))
        )
        backend.deliveredIds = DeliveredQuery.Known(setOf(stillUp.id))

        notifier.reconcileNotifications()

        assertEquals(listOf(stillUp.id), notificationDao.getAll())
    }

    @Test
    fun rowsAreDroppedWhenNothingCanConfirmWhatIsOnScreen() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))

        backend.deliveredIds = DeliveredQuery.Unknown

        notifier.reconcileNotifications()

        assertTrue(backend.shown.isEmpty())
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun whatACleanShutdownTookDownIsRestored() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        notificationDao.setPlatformId(task.id, 42L)
        screenCleared = true
        backend.deliveredIds = DeliveredQuery.Unknown
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        val shown = backend.shown.single()
        assertEquals(task.id, shown.taskId)
        assertEquals(Alert.SUPPRESSED, shown.alert)
        assertEquals(listOf(task.id), notificationDao.getAll())
        assertTrue(backend.dismissed.isEmpty())
    }

    @Test
    fun whatAKilledRunLeftBehindIsClosedRatherThanRestored() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        notificationDao.setPlatformId(task.id, 42L)
        screenCleared = false
        backend.deliveredIds = DeliveredQuery.Unknown
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        assertEquals(listOf(listOf(task.id)), backend.dismissed)
        assertTrue(backend.shown.isEmpty())
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun theShutdownMarkIsSpentOnTheFirstReconcile() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        screenCleared = true
        backend.deliveredIds = DeliveredQuery.Unknown
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        assertTrue(!screenCleared)
    }

    @Test
    fun aCleanShutdownRecordsThatTheScreenWasCleared() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.shutdown()

        assertTrue(screenCleared)
    }

    @Test
    fun aShutdownThatNeverBuiltABackendDoesNotClaimTheScreenWasCleared() = runTest {
        notifier.shutdown()

        assertTrue(!screenCleared)
    }

    @Test
    fun aBackendThatCanEnumerateIsNotAskedAboutAScreenWeCleared() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        screenCleared = true
        backend.deliveredIds = DeliveredQuery.Known(emptySet())
        backend.actionsSurvive = false

        notifier.reconcileNotifications()

        assertEquals(task.id, backend.shown.single().taskId)
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun aBackendThatLeavesNotificationsUpDoesNotClaimTheScreenWasCleared() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.clearsOnShutdown = false
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.shutdown()

        assertTrue(!screenCleared)
    }

    @Test
    fun startingUpWithNothingOutstandingDoesNotEvenBuildABackend() = runTest {
        notifier.reconcileNotifications()

        assertTrue(built.isEmpty())
    }

    @Test
    fun whatSurvivedARestartCountsAgainstTheCap() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val existing = (1..MAX_NOTIFICATIONS).map {
            createTask(Task(title = "from last time $it"))
        }
        notificationDao.insertAll(
            existing.mapIndexed { index, task ->
                Notification(
                    taskId = task.id,
                    timestamp = NOW - index * 60_000L,
                    type = Alarm.TYPE_DATE_TIME,
                )
            }
        )

        backend.deliveredIds = DeliveredQuery.Known(existing.map { it.id }.toSet())
        notifier.reconcileNotifications()
        assertEquals(existing.size, notificationDao.getAll().size)

        val fresh = createTask(Task(title = "due now"))
        notifier.triggerNotifications(listOf(notification(fresh.id)))

        assertEquals(listOf(listOf(existing.last().id)), backend.dismissed)
        assertEquals(MAX_NOTIFICATIONS, notificationDao.getAll().size)
    }

    @Test
    fun anUnansweredPermissionPromptIsAskedAgain() = runTest {
        val task = createTask(Task(title = "Water the plants"))
        alarmDao.insert(Alarm(task = task.id, time = NOW, type = Alarm.TYPE_DATE_TIME))
        backend.permission = NotificationPermission.NOT_DETERMINED

        backend.answersPermissionPrompt = false

        notifier.requestPermissionIfNeeded()
        assertEquals(1, backend.permissionRequests)

        notifier.requestPermissionIfNeeded()
        assertEquals(2, backend.permissionRequests)

        backend.answersPermissionPrompt = true
        notifier.requestPermissionIfNeeded()
        assertEquals(3, backend.permissionRequests)

        repeat(5) { notifier.requestPermissionIfNeeded() }
        assertEquals(3, backend.permissionRequests)
    }

    @Test
    fun aFailedRepostKeepsTheRowTheNotificationOnScreenNeeds() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))
        assertEquals(listOf(task.id), notificationDao.getAll())

        backend.posts = false
        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(delivered.isEmpty())
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun aFailedFirstPostLeavesNoRowBehind() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.posts = false

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun onlyTheCapStaysOnScreen() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map {
            createTask(Task(title = "task $it"))
        }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(
            tasks.drop(4).map { it.id }.toSet(),
            notificationDao.getAll().toSet(),
        )
    }

    @Test
    fun aBatchOverTheCapIsTrimmedBeforeAnythingIsPosted() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map {
            createTask(Task(title = "task $it"))
        }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(
            tasks.drop(4).map { it.id },
            backend.shown.map { it.taskId },
        )
        assertTrue(backend.dismissed.isEmpty())
    }

    @Test
    fun remindersDroppedForTheCapAreStillRetired() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map {
            createTask(Task(title = "task $it"))
        }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        tasks.take(4).forEach {
            assertEquals(NOW.endOfMinute(), taskDao.fetch(it.id)?.reminderLast)
        }
    }

    @Test
    fun remindersDroppedForTheCapAreNotReportedAsDelivered() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map {
            createTask(Task(title = "task $it"))
        }

        val delivered = notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(tasks.drop(4).map { it.id }.toSet(), delivered.toSet())
        assertTrue(tasks.take(4).none { it.id in delivered })
    }

    @Test
    fun theCapKeepsTheNewestRatherThanTheHighestTaskIds() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map { createTask(Task(title = "task $it")) }

        val oldestLast = tasks.reversed()

        notifier.triggerNotifications(
            oldestLast.map { notification(it.id) }
        )

        assertEquals(
            oldestLast.drop(4).map { it.id },
            backend.shown.map { it.taskId },
        )
    }

    @Test
    fun cancelKeepsTheRowWhenTheBackendCannotTakeTheNotificationDown() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))
        backend.dismisses = false

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertEquals(listOf(listOf(task.id)), backend.dismissed)
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun cancelKeepsTheRowWhenThereIsNoBackendToDismissThrough() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertTrue(built.isEmpty())
        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun reconcileTakesDownNotificationsForTasksDealtWithWhileClosed() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val completed = createTask(Task(title = "done on the phone", completionDate = NOW))
        notificationDao.insertAll(listOf(notification(completed.id)))

        notifier.reconcileNotifications()

        assertEquals(listOf(listOf(completed.id)), backend.dismissed)
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun reconcileLeavesAloneWhatTheScanRePostedWhileItWasRunning() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val stillUp = createTask(Task(title = "Water the plants"))
        val rePosted = createTask(Task(title = "every morning"))
        notificationDao.insertAll(listOf(notification(stillUp.id), notification(rePosted.id)))

        backend.deliveredIds = DeliveredQuery.Known(setOf(stillUp.id))

        backend.onDelivered = {
            notificationDao.insertAll(
                listOf(
                    Notification(
                        taskId = rePosted.id,
                        timestamp = NOW + 60_000L,
                        type = Alarm.TYPE_DATE_TIME,
                    )
                )
            )
        }

        notifier.reconcileNotifications()

        assertEquals(setOf(stillUp.id, rePosted.id), notificationDao.getAll().toSet())

        assertTrue(backend.dismissed.isEmpty())
    }

    @Test
    fun reconcileLeavesAloneWhatTheScanRePostedWhileItWasDismissing() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val rePosted = createTask(Task(title = "every morning"))
        notificationDao.insertAll(listOf(notification(rePosted.id)))

        backend.deliveredIds = DeliveredQuery.Known(emptySet())

        backend.onDismiss = {
            notificationDao.insertAll(
                listOf(
                    Notification(
                        taskId = rePosted.id,
                        timestamp = NOW + 60_000L,
                        type = Alarm.TYPE_DATE_TIME,
                    )
                )
            )
        }

        notifier.reconcileNotifications()

        assertEquals(listOf(rePosted.id), notificationDao.getAll())
    }

    @Test
    fun closingTheNotifierClosesTheBackend() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.close()

        assertEquals(1, backend.closes)
    }

    @Test
    fun closingWithoutABackendIsHarmless() = runTest {
        notifier.close()

        assertTrue(built.isEmpty())
    }

    @Test
    fun nothingIsBuiltAfterClose() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.close()

        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(built.isEmpty())

        assertTrue(delivered.isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)?.reminderLast)
    }

    @Test
    fun aBackendBuiltAcrossCloseIsClosedRatherThanKept() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        nextBackend = {
            notifier.close()
            RecordingNotifications()
        }

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(1, built.size)
        assertEquals(1, built.single().closes)
    }

    @Test
    fun anUntitledTaskStillGetsSomethingOnScreen() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val untitled = createTask(Task(title = null))
        val blank = createTask(Task(title = "   "))

        notifier.triggerNotifications(
            listOf(notification(untitled.id), notification(blank.id))
        )

        assertEquals(2, backend.shown.size)
        assertTrue(backend.shown.all { it.title.isNotBlank() })
    }

    @Test
    fun aLongNoteIsCutDownBeforeItReachesTheBackend() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(
            Task(title = "t".repeat(10_000), notes = "n".repeat(100_000))
        )

        notifier.triggerNotifications(listOf(notification(task.id)))

        val shown = backend.shown.single()
        assertTrue(shown.title.length <= NotificationContent.MAX_TITLE_LENGTH + 1)
        assertTrue(shown.body!!.length <= NotificationContent.MAX_BODY_LENGTH + 1)
    }

    @Test
    fun truncationLeavesOrdinaryTextAlone() {
        assertEquals("Water the plants", NotificationContent.truncate("Water the plants", 200))
    }

    @Test
    fun truncationDoesNotSplitASurrogatePair() {
        val value = "ab\uD83C\uDF31"

        val truncated = NotificationContent.truncate(value, 3)

        assertEquals("ab…", truncated)
    }

    @Test
    fun cancelDeletesOnlyTheRowsThatWereActuallyTakenDown() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }
        notifier.triggerNotifications(tasks.map { notification(it.id) })
        backend.undismissable.add(tasks[1].id)

        notifier.cancel(tasks.map { it.id }, CancelReason.COMPLETE)

        assertEquals(listOf(tasks[1].id), notificationDao.getAll())
    }

    @Test
    fun cancelKeepsARowThatWasRepostedWhileItWasDismissing() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        backend.onDismiss = {
            notificationDao.insertAll(
                listOf(
                    Notification(
                        taskId = task.id,
                        timestamp = NOW + 60_000L,
                        type = Alarm.TYPE_DATE_TIME,
                    )
                )
            )
        }

        notifier.cancel(task.id, CancelReason.COMPLETE)

        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun aBackendBuiltAfterCloseIsNotPostedThrough() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))

        nextBackend = {
            notifier.close()
            RecordingNotifications()
        }

        val delivered = notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(1, built.size)
        val discarded = built.single()
        assertEquals(1, discarded.closes)

        assertEquals(0, discarded.showAttempts)
        assertTrue(discarded.closedDuringShow.isEmpty())
        assertTrue(delivered.isEmpty())

        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)
    }

    @Test
    fun aCancelDuringABatchCannotOrphanAnEntryThatHasNotBeenPostedYet() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val first = createTask(Task(title = "one"))
        val second = createTask(Task(title = "two"))

        backend.onShow = {
            backend.onShow = null
            notifier.cancel(second.id, CancelReason.COMPLETE)
        }

        notifier.triggerNotifications(listOf(notification(first.id), notification(second.id)))

        assertEquals(listOf(first.id, second.id), backend.shown.map { it.taskId })
        assertTrue(second.id in notificationDao.getAll())
    }

    @Test
    fun remindersAreHeldWhileTheyAreSwitchedOff() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        var enabled = false
        val switched = DesktopNotifier(
            taskDao = taskDao,
            notificationDao = notificationDao,
            alarmDao = alarmDao,
            refreshBroadcaster = refreshBroadcaster,
            signalScheduler = {},
            notificationsEnabled = { enabled },
            createBackend = { nextBackend()?.also { built.add(it) } },
        )

        assertTrue(switched.triggerNotifications(listOf(notification(task.id))).isEmpty())

        assertTrue(built.isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)!!.reminderLast)

        enabled = true
        switched.triggerNotifications(listOf(notification(task.id)))

        assertEquals(1, backend.shown.size)
        assertEquals(NOW.endOfMinute(), taskDao.fetch(task.id)!!.reminderLast)
    }

    @Test
    fun overCapRemindersAreNotRetiredWhenTheBatchDeliveredNothing() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map { createTask(Task(title = "task $it")) }
        backend.throwOnShow = true

        val delivered = notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertTrue(delivered.isEmpty())

        tasks.forEach { assertEquals(0L, taskDao.fetch(it.id)?.reminderLast) }
    }

    @Test
    fun overCapRemindersAreNotRetiredWhenPartOfTheBatchFailed() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map { createTask(Task(title = "task $it")) }

        backend.onShow = { if (backend.showAttempts > 1) backend.throwOnShow = true }

        val delivered = notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(listOf(tasks[4].id), delivered)

        tasks.take(4).forEach { assertEquals(0L, taskDao.fetch(it.id)?.reminderLast) }

        assertEquals(NOW.endOfMinute(), taskDao.fetch(tasks[4].id)?.reminderLast)
    }

    @Test
    fun overCapRemindersAreRetiredOnceTheBatchHasGoneOut() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..MAX_NOTIFICATIONS + 4).map { createTask(Task(title = "task $it")) }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        tasks.take(4).forEach {
            assertEquals(NOW.endOfMinute(), taskDao.fetch(it.id)?.reminderLast)
        }
    }

    @Test
    fun onlyTheFirstReminderOfABatchMakesASound() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(
            listOf(Alert.DEFAULT, Alert.QUIET, Alert.QUIET),
            backend.shown.map { it.alert },
        )
    }

    @Test
    fun aBatchThatIsAllRefusalsStillOnlyMakesOneSound() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }

        backend.posts = false

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(
            listOf(Alert.DEFAULT, Alert.QUIET, Alert.QUIET),
            backend.alerts,
        )
    }

    @Test
    fun aRefusedFirstPostSpendsTheBatchesOneSound() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }
        backend.posts = false
        backend.onShow = { if (backend.showAttempts > 1) backend.posts = true }

        notifier.triggerNotifications(tasks.map { notification(it.id) })

        assertEquals(listOf(Alert.DEFAULT, Alert.QUIET, Alert.QUIET), backend.alerts)

        assertEquals(tasks.drop(1).map { it.id }, backend.shown.map { it.taskId })
        assertEquals(listOf(Alert.QUIET, Alert.QUIET), backend.shown.map { it.alert })
    }

    @Test
    fun aSingleReminderAlertsNormally() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(Alert.DEFAULT, backend.shown.single().alert)
    }

    @Test
    fun cancelAllTakesDownEverythingOnScreen() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }
        notifier.triggerNotifications(tasks.map { notification(it.id) })
        assertEquals(3, notificationDao.getAll().size)

        notifier.cancelAll(CancelReason.DISABLED)

        assertEquals(tasks.map { it.id }.toSet(), backend.dismissed.flatten().toSet())
        assertEquals(emptyList<Long>(), notificationDao.getAll())

        tasks.forEach { assertEquals(NOW.endOfMinute(), taskDao.fetch(it.id)?.reminderLast) }
    }

    @Test
    fun cancelAllBuildsABackendWhenNothingIsCached() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }

        notificationDao.insertAll(tasks.map { notification(it.id) })

        notifier.cancelAll(CancelReason.DISABLED)

        assertEquals(1, built.size)
        assertEquals(tasks.map { it.id }.toSet(), backend.dismissed.flatten().toSet())
        assertEquals(emptyList<Long>(), notificationDao.getAll())
    }

    @Test
    fun cancelAllBuildsNothingWhenThereIsNothingUp() = runTest {
        notifier.cancelAll(CancelReason.DISABLED)

        assertTrue(built.isEmpty())
    }

    @Test
    fun cancelAllDismissesThroughTheBackendThatPostedThem() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }

        notifier.triggerNotifications(tasks.map { notification(it.id) })
        assertEquals(3, notificationDao.getAll().size)
        val builtToPost = built.size

        notifier.cancelAll(CancelReason.DISABLED)

        assertEquals(tasks.map { it.id }.toSet(), backend.dismissed.flatten().toSet())
        assertEquals(emptyList<Long>(), notificationDao.getAll())

        assertEquals(builtToPost, built.size)
    }

    @Test
    fun reconcileDoesNotBuildABackendWhileRemindersAreOff() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notificationDao.insertAll(listOf(notification(task.id)))
        val switched = DesktopNotifier(
            taskDao = taskDao,
            notificationDao = notificationDao,
            alarmDao = alarmDao,
            refreshBroadcaster = refreshBroadcaster,
            signalScheduler = {},
            notificationsEnabled = { false },
            createBackend = { nextBackend()?.also { built.add(it) } },
        )

        switched.reconcileNotifications()

        assertTrue(built.isEmpty())

        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun theTableAgreesWithWhatIsActuallyOnScreen() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..5).map { createTask(Task(title = "task $it")) }

        notifier.triggerNotifications(tasks.take(3).map { notification(it.id) })
        assertEquals(backend.onScreen(), notificationDao.getAll().toSet())

        notifier.cancel(tasks[0].id, CancelReason.COMPLETE)
        assertEquals(backend.onScreen(), notificationDao.getAll().toSet())

        backend.posts = false
        notifier.triggerNotifications(listOf(notification(tasks[3].id)))
        backend.posts = true
        assertEquals(backend.onScreen(), notificationDao.getAll().toSet())

        backend.clear(tasks[1].id)
        notifier.cancel(tasks[1].id, CancelReason.DISMISS)
        assertEquals(backend.onScreen(), notificationDao.getAll().toSet())

        assertEquals(setOf(tasks[2].id), notificationDao.getAll().toSet())
    }

    @Test
    fun cancellationUnwindsOutOfTheBatchRatherThanBeingRecorded() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val tasks = (1..3).map { createTask(Task(title = "task $it")) }
        backend.onShow = {
            if (backend.showAttempts == 2) {
                throw CancellationException("quitting")
            }
        }

        val thrown = runCatching {
            notifier.triggerNotifications(tasks.map { notification(it.id) })
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)

        assertEquals(NOW.endOfMinute(), taskDao.fetch(tasks[0].id)!!.reminderLast)
        assertEquals(0L, taskDao.fetch(tasks[1].id)!!.reminderLast)
        assertEquals(0L, taskDao.fetch(tasks[2].id)!!.reminderLast)

        assertEquals(1, built.size)
    }

    @Test
    fun recordsThePlatformIdOfAPostedNotification() = runTest {
        val task = createTask(Task())
        backend.platformIds[task.id] = 42L

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(42L, notificationDao.get(task.id)!!.platformId)
    }

    @Test
    fun leavesThePlatformIdUnsetWhenThePlatformHasNone() = runTest {
        val task = createTask(Task())

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(null, notificationDao.get(task.id)!!.platformId)
    }

    @Test
    fun handsRecordedPlatformIdsBackWhenABackendIsBuilt() = runTest {
        val task = createTask(Task())
        notificationDao.insertAll(listOf(notification(task.id)))
        notificationDao.setPlatformId(task.id, 42L)

        notifier.reconcileNotifications()

        assertEquals(listOf(mapOf(task.id to 42L)), backend.adopted)
    }

    @Test
    fun doesNotHandBackAnythingWhenNoPlatformIdWasRecorded() = runTest {
        val task = createTask(Task())
        notificationDao.insertAll(listOf(notification(task.id)))

        notifier.reconcileNotifications()

        assertEquals(emptyList<Map<Long, Long>>(), backend.adopted)
    }

    @Test
    fun aBackendThatCouldNotBeBuiltIsTriedAgainOnceTheIntervalHasPassed() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        nextBackend = { null }

        assertTrue(notifier.triggerNotifications(listOf(notification(task.id))).isEmpty())
        assertEquals(0L, taskDao.fetch(task.id)?.reminderLast)

        nextBackend = { backend }
        elapsed += DesktopNotifier.BACKEND_RETRY_MS

        assertEquals(listOf(task.id), notifier.triggerNotifications(listOf(notification(task.id))))
        assertEquals(listOf(task.id), backend.shown.map { it.taskId })
    }

    @Test
    fun aFailedBuildIsNotRetriedOnEveryScan() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        var attempts = 0
        nextBackend = {
            attempts++
            null
        }

        repeat(5) { notifier.triggerNotifications(listOf(notification(task.id))) }

        assertEquals(1, attempts)
    }

    @Test
    fun idsRecordedByAnEarlierRunAreHandedBackToTheBackend() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val earlier = createTask(Task(title = "from last time"))
        notificationDao.insertAll(listOf(notification(earlier.id)))
        notificationDao.setPlatformId(earlier.id, 42L)
        val task = createTask(Task(title = "Water the plants"))

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertEquals(listOf(mapOf(earlier.id to 42L)), backend.adopted)
    }

    @Test
    fun idsFromASessionThatIsOverAreDiscardedRatherThanAimedAtWhoeverHoldsThemNow() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val earlier = createTask(Task(title = "from last time"))
        notificationDao.insertAll(listOf(notification(earlier.id)))
        notificationDao.setPlatformId(earlier.id, 42L)
        val task = createTask(Task(title = "Water the plants"))
        platformIdsAreOurs = false

        notifier.triggerNotifications(listOf(notification(task.id)))

        assertTrue(backend.adopted.isEmpty())
        assertTrue(notificationDao.withPlatformIds().isEmpty())
    }

    @Test
    fun idsAreForgottenWhenTheBackendTookItsNotificationsDownWithIt() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.platformIds[task.id] = 42L
        notifier.triggerNotifications(listOf(notification(task.id)))
        assertEquals(42L, notificationDao.get(task.id)?.platformId)

        notifier.shutdown()

        assertTrue(notificationDao.withPlatformIds().isEmpty())

        assertEquals(listOf(task.id), notificationDao.getAll())
    }

    @Test
    fun idsAreKeptWhereTheBackendLeftItsNotificationsUp() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        backend.platformIds[task.id] = 42L
        backend.clearsOnShutdown = false
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.shutdown()

        assertEquals(42L, notificationDao.get(task.id)?.platformId)
    }

    @Test
    fun aDeletedTaskComesOffScreenEvenThoughItsRowWentWithIt() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notificationDao.deleteAll(listOf(task.id))
        notifier.cancelDeleted(listOf(task.id))

        assertEquals(listOf(listOf(task.id)), backend.dismissed)
        assertTrue(backend.onScreen().isEmpty())
    }

    @Test
    fun aSoftDeletedTaskLosesItsRowAsWellAsItsNotification() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        notifier.cancelDeleted(listOf(task.id))

        assertEquals(listOf(listOf(task.id)), backend.dismissed)
        assertTrue(notificationDao.getAll().isEmpty())
    }

    @Test
    fun aDeletionBuildsNothingWhereNothingWasEverPosted() = runTest {
        notifier.cancelDeleted(listOf(1L, 2L))

        assertTrue(built.isEmpty())
    }

    @Test
    fun scanningIsHeldUntilRemindersAreSwitchedBackOn() = runTest {
        var enabled = false
        val switched = DesktopNotifier(
            taskDao = taskDao,
            notificationDao = notificationDao,
            alarmDao = alarmDao,
            refreshBroadcaster = refreshBroadcaster,
            signalScheduler = {},
            notificationsEnabled = { enabled },
            createBackend = { nextBackend()?.also { built.add(it) } },
        )

        assertEquals(Hold.UNTIL_SIGNALLED, switched.hold())

        enabled = true
        assertEquals(Hold.NONE, switched.hold())
    }

    @Test
    fun scanningIsHeldUntilTheNextAlarmWhilePermissionIsRefused() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        notifier.triggerNotifications(listOf(notification(task.id)))

        backend.permission = NotificationPermission.DENIED
        assertEquals(Hold.UNTIL_NEXT_ALARM, notifier.hold())

        backend.permission = NotificationPermission.NOT_DETERMINED
        assertEquals(Hold.UNTIL_NEXT_ALARM, notifier.hold())

        backend.permission = NotificationPermission.GRANTED
        assertEquals(Hold.NONE, notifier.hold())
    }

    @Test
    fun nothingIsHeldBeforeABackendHasBeenBuilt() = runTest {
        backend.permission = NotificationPermission.DENIED

        assertEquals(Hold.NONE, notifier.hold())
        assertTrue(built.isEmpty())
    }

    @Test
    fun nothingIsHeldWherePermissionIsNotAGate() = runTest {
        DateTimeUtils2.setCurrentMillisFixed(NOW)
        val task = createTask(Task(title = "Water the plants"))
        val ungated = DesktopNotifier(
            taskDao = taskDao,
            notificationDao = notificationDao,
            alarmDao = alarmDao,
            refreshBroadcaster = refreshBroadcaster,
            signalScheduler = {},
            gatesOnPermission = { false },
            createBackend = { nextBackend()?.also { built.add(it) } },
        )
        ungated.triggerNotifications(listOf(notification(task.id)))

        backend.permission = NotificationPermission.DENIED

        assertEquals(Hold.NONE, ungated.hold())
    }

    @Test
    fun answeringThePermissionPromptWakesTheScan() = runTest {
        val task = createTask(Task(title = "Water the plants"))
        alarmDao.insert(Alarm(task = task.id, time = NOW, type = Alarm.TYPE_DATE_TIME))
        backend.permission = NotificationPermission.NOT_DETERMINED
        var signals = 0
        val prompting = DesktopNotifier(
            taskDao = taskDao,
            notificationDao = notificationDao,
            alarmDao = alarmDao,
            refreshBroadcaster = refreshBroadcaster,
            signalScheduler = { signals++ },
            createBackend = { nextBackend()?.also { built.add(it) } },
        )

        prompting.requestPermissionIfNeeded()

        assertEquals(1, backend.permissionRequests)
        assertEquals(1, signals)
    }

    private suspend fun createTask(task: Task): Task = task.also { taskDao.createNew(it) }

    private fun notification(taskId: Long) =
        Notification(taskId = taskId, timestamp = NOW, type = Alarm.TYPE_DATE_TIME)

    private class RecordingNotifications : PlatformNotifications {
        var actions = true
        override val supportsActions: Boolean get() = actions

        val shown = mutableListOf<Shown>()
        val dismissed = mutableListOf<List<Long>>()

        private val up = mutableSetOf<Long>()

        fun onScreen(): Set<Long> = up.toSet()

        fun clear(taskId: Long) {
            up.remove(taskId)
        }
        var permission = NotificationPermission.GRANTED
        var permissionRequests = 0

        var opens = true
        override val supportsOpen: Boolean get() = opens

        var platformIds = mutableMapOf<Long, Long>()
        val adopted = mutableListOf<Map<Long, Long>>()

        override suspend fun platformId(taskId: Long): Long? = platformIds[taskId]

        override suspend fun adopt(platformIds: Map<Long, Long>) {
            adopted.add(platformIds)
            this.platformIds.putAll(platformIds)
        }

        var deliveredIds: DeliveredQuery = DeliveredQuery.Known(emptySet())

        var onDelivered: (suspend () -> Unit)? = null

        override suspend fun delivered(): DeliveredQuery {
            onDelivered?.invoke()
            return deliveredIds
        }

        var throwOnDismiss = false

        var dismisses = true

        val undismissable = mutableSetOf<Long>()

        var posts = true

        var throwOnShow = false

        var throwOnPermission = false

        var answersPermissionPrompt = true

        val closed: Boolean get() = closes > 0

        override suspend fun permission(): NotificationPermission {
            if (throwOnPermission) {
                throw UnsatisfiedLinkError("no UserNotifications framework")
            }
            return permission
        }

        override suspend fun requestPermission(): Boolean {
            permissionRequests++
            return answersPermissionPrompt
        }

        var actionsSurvive = true
        override val actionsSurviveRestart: Boolean get() = actionsSurvive

        var onShow: (suspend () -> Unit)? = null

        val closedDuringShow = mutableListOf<Long>()

        var showAttempts = 0

        val alerts = mutableListOf<Alert>()

        override suspend fun show(
            taskId: Long,
            title: String,
            body: String?,
            actions: List<NotificationAction>,
            alert: Alert,
        ): Boolean {
            showAttempts++
            alerts.add(alert)
            if (closed) {
                closedDuringShow.add(taskId)
            }
            onShow?.invoke()
            if (throwOnShow) {
                throw IllegalStateException("no session bus")
            }
            if (!posts) {
                return false
            }
            shown.add(Shown(taskId, title, body, actions, alert))
            up.add(taskId)
            return true
        }

        var onDismiss: (suspend () -> Unit)? = null

        override suspend fun dismiss(taskIds: List<Long>): Set<Long> {
            if (throwOnDismiss) {
                throw IllegalStateException("no session bus")
            }
            onDismiss?.invoke()
            dismissed.add(taskIds)
            return if (dismisses) {
                taskIds.filterNot { it in undismissable }.toSet().also { up.removeAll(it) }
            } else {
                emptySet()
            }
        }

        override fun close(): Boolean {
            closes++
            return clearsOnShutdown
        }

        var clearsOnShutdown = true

        var closes = 0
            private set

        data class Shown(
            val taskId: Long,
            val title: String,
            val body: String?,
            val actions: List<NotificationAction>,
            val alert: Alert = Alert.DEFAULT,
        )
    }

    companion object {
        private val NOW = 1_700_000_000_000L.startOfMinute()
    }
}
