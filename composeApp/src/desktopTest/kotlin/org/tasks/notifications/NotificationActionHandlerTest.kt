package org.tasks.notifications

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.tasks.DatabaseTest
import org.tasks.TaskRequests
import org.tasks.data.entity.Task
import org.tasks.service.TaskCompleter
import java.util.concurrent.CopyOnWriteArrayList

class NotificationActionHandlerTest : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val notifier = mock<Notifier>()
    private val taskCompleter = mock<TaskCompleter>()
    private val taskRequests = TaskRequests()
    private var foregroundRequests = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val handler = NotificationActionHandler(
        scope = scope,
        taskDao = taskDao,
        taskCompleter = taskCompleter,
        notifier = { notifier },
        taskRequests = taskRequests,
        requestForeground = { foregroundRequests++ },
    )

    @After
    fun stopScope() {
        scope.cancel()
    }

    @Test
    fun openingClearsTheNotificationOnlyOnceTheTaskIsOnScreen() = runTest {
        val task = createTask()
        val answering = answerOpenWith(true)

        handler.onAction(task.id, NotificationAction.OPEN)
        answering.join()
        idle()

        assertEquals(1, foregroundRequests)
        verifyBlocking(notifier) { cancel(task.id, CancelReason.DISMISS) }
    }

    @Test
    fun aRefusedOpenLeavesTheNotificationUp() = runTest {
        val task = createTask()
        val answering = answerOpenWith(false)

        handler.onAction(task.id, NotificationAction.OPEN)
        answering.join()
        idle()

        assertEquals(1, foregroundRequests)
        verifyBlocking(notifier, never()) { cancel(task.id, CancelReason.DISMISS) }
    }

    @Test
    fun anOpenIsDeclinedOutrightWhileTheAppIsShuttingDown() = runTest {
        val task = createTask()
        val answering = answerOpenWith(true)
        taskRequests.acceptOpenRequests(false)

        handler.onAction(task.id, NotificationAction.OPEN)

        idle(answering)

        verifyBlocking(notifier, never()) { cancel(task.id, CancelReason.DISMISS) }

        assertTrue(answering.isActive)
        answering.cancel()
    }

    @Test
    fun snoozingAsksTheAppAndLeavesTheNotificationUp() = runTest {
        val task = createTask()

        handler.onAction(task.id, NotificationAction.SNOOZE)

        assertEquals(task.id, taskRequests.snoozeRequests.first())
        assertEquals(1, foregroundRequests)
        verifyBlocking(notifier, never()) { cancel(task.id, CancelReason.DISMISS) }
    }

    @Test
    fun completingGoesThroughTaskCompleter() = runTest {
        val task = createTask()

        handler.onAction(task.id, NotificationAction.COMPLETE)
        idle()

        verifyBlocking(taskCompleter) { setComplete(task.id) }

        assertEquals(0, foregroundRequests)
    }

    @Test
    fun dismissingClearsTheRow() = runTest {
        handler.onDismissed(42L)
        idle()

        verifyBlocking(notifier) { cancel(42L, CancelReason.DISMISS) }
    }

    @Test
    fun anActionOnATaskThatIsGoneDoesNothing() = runTest {
        handler.onAction(404L, NotificationAction.OPEN)
        handler.onAction(404L, NotificationAction.SNOOZE)
        handler.onAction(404L, NotificationAction.COMPLETE)
        idle()

        assertEquals(0, foregroundRequests)
        verifyBlocking(notifier, never()) { cancel(404L, CancelReason.DISMISS) }

        verifyBlocking(taskCompleter) { setComplete(404L) }
    }

    @Test
    fun aFailedActionDoesNotEscapeTheHandlersScope() = runTest {
        val task = createTask()
        val failingCompleter = mock<TaskCompleter> {
            onBlocking { setComplete(any<Long>(), any<Boolean>()) } doThrow
                    IllegalStateException("database is locked")
        }
        val failing = NotificationActionHandler(
            scope = scope,
            taskDao = taskDao,
            taskCompleter = failingCompleter,
            notifier = { error("the transport is gone") },
            taskRequests = taskRequests,
            requestForeground = { error("no display") },
        )
        val uncaught = CopyOnWriteArrayList<Throwable>()
        val installed = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught.add(e) }

        try {
            failing.onAction(task.id, NotificationAction.COMPLETE)
            failing.onAction(task.id, NotificationAction.SNOOZE)
            failing.onAction(task.id, NotificationAction.OPEN)
            failing.onDismissed(task.id)
            idle()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(installed)
        }

        verifyBlocking(failingCompleter) { setComplete(task.id) }
        assertTrue(
            "a failed action reached the uncaught handler: " +
                    uncaught.joinToString { it.toString() },
            uncaught.isEmpty(),
        )
    }

    private suspend fun answerOpenWith(accepted: Boolean): Job {
        val subscribed = CompletableDeferred<Unit>()
        val answering = scope.launch {
            taskRequests.openRequests
                .onSubscription { subscribed.complete(Unit) }
                .first()
                .complete(accepted)
        }
        subscribed.await()
        return answering
    }

    private suspend fun idle(vararg ignoring: Job) =
        scope.coroutineContext.job.children.filter { it !in ignoring }.toList().joinAll()

    private suspend fun createTask(): Task = Task().also { taskDao.createNew(it) }
}
