package org.tasks.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.jetbrains.compose.resources.getString
import org.tasks.R
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.pickers.QuickPickTimes
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.jobs.WorkManager
import org.tasks.markdown.Markdown
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.rmd_NoA_done
import tasks.kmp.generated.resources.rmd_NoA_snooze

@RunWith(RobolectricTestRunner::class)
class NotificationManagerCharacterizationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences: Preferences = mock()
    private val notificationDao: NotificationDao = mock()
    private val taskDao: TaskDao = mock()
    private val locationDao: LocationDao = mock()
    private val refreshBroadcaster: RefreshBroadcaster = mock()
    private val markdownProvider: MarkdownProvider = mock()
    private val permissionChecker: PermissionChecker = mock()
    private val workManager: WorkManager = mock()
    private val throttled: ThrottledNotificationManager = mock()

    private val shade = mutableListOf<String>()

    private val built = mutableMapOf<Int, android.app.Notification>()

    private val tasks = mutableMapOf<Long, Task>()

    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        throttled.stub {
            on { notify(any(), any()) } doAnswer {
                val id = it.arguments[0] as Int
                shade.add("notify:$id")
                built[id] = it.arguments[1] as android.app.Notification
                Unit
            }
            on { cancel(any()) } doAnswer {
                shade.add("cancel:${it.arguments[0]}")
                Unit
            }
        }
        preferences.stub {
            on { bundleNotifications() } doReturn false
            on { usePersistentReminders() } doReturn false
            on { quickPickTimes } doReturn QuickPickTimes()
            on { getBoolean(any<Int>(), any()) } doReturn false
        }
        markdownProvider.stub {
            on { markdown(anyBoolean(), anyBoolean()) } doReturn PassThroughMarkdown
        }
        whenever(permissionChecker.canNotify()) doReturn true
        taskDao.stub {
            onBlocking { fetch(any<Long>()) } doAnswer { tasks[it.arguments[0] as Long] }
            onBlocking { activeNotifications() } doAnswer { tasks.values.toList() }
        }
        notificationDao.stub {
            onBlocking { getAllOrdered() } doReturn emptyList()
            onBlocking { getAll() } doReturn emptyList()
            onBlocking { latestTimestamp() } doReturn null
        }
        notificationManager = NotificationManager(
            context = context,
            preferences = preferences,
            notificationDao = notificationDao,
            taskDao = taskDao,
            locationDao = locationDao,
            refreshBroadcaster = refreshBroadcaster,
            notificationManager = throttled,
            markdownProvider = markdownProvider,
            permissionChecker = permissionChecker,
            workManager = workManager,
        )
    }

    @Test
    fun permissionDeniedRecordsNothingAtAll() = runTest {
        whenever(permissionChecker.canNotify()) doReturn false
        givenTask(1L, "water the plants")

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        assertEquals(emptyList<String>(), shade)
        verifyBlocking(notificationDao, never()) { insertAll(any()) }
        verify(refreshBroadcaster, never()).broadcastRefresh()
    }

    @Test
    fun everyNotificationWithABuilderIsRecordedAsNotified() = runTest {
        givenTask(1L, "one")
        givenTask(2L, "two")

        notificationManager.notifyTasks(
            listOf(notification(1L, TIMESTAMP), notification(2L, TIMESTAMP)),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        assertEquals(listOf("notify:1", "notify:2"), shade)
        val endOfMinute = DateTime(TIMESTAMP).endOfMinute().millis
        verifyBlocking(taskDao) { setLastNotified(1L, endOfMinute) }
        verifyBlocking(taskDao) { setLastNotified(2L, endOfMinute) }
    }

    @Test
    fun aNotificationForAMissingTaskIsCancelledAndItsRowDeleted() = runTest {
        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        assertEquals(listOf("cancel:1"), shade)
        verifyBlocking(notificationDao) { delete(1L) }
        verifyBlocking(taskDao, never()) { setLastNotified(any(), any()) }
    }

    @Test
    fun aNotificationForACompletedTaskIsCancelledAndItsRowDeleted() = runTest {
        givenTask(1L, "done already", completionDate = TIMESTAMP)

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        assertEquals(listOf("cancel:1"), shade)
        verifyBlocking(notificationDao) { delete(1L) }
    }

    @Test
    fun onlyTheFirstNotificationOfABatchAlerts() = runTest {
        givenTask(1L, "one")
        givenTask(2L, "two")
        givenTask(3L, "three")

        notificationManager.notifyTasks(
            listOf(notification(1L), notification(2L), notification(3L)),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        assertEquals(NotificationCompat.GROUP_ALERT_CHILDREN, alertBehaviour(1))
        assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, alertBehaviour(2))
        assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, alertBehaviour(3))
    }

    @Test
    fun aStaleNotificationDoesNotSpendTheBatchesAlert() = runTest {
        givenTask(2L, "two")

        notificationManager.notifyTasks(
            listOf(notification(1L), notification(2L)),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        assertEquals(listOf("cancel:1", "notify:2"), shade)
        assertEquals(NotificationCompat.GROUP_ALERT_CHILDREN, alertBehaviour(2))
    }

    @Test
    fun postingDoesNotDeleteRows() = runTest {
        givenTask(1L, "one")

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        verifyBlocking(notificationDao, never()) { deleteAll(any()) }
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun restoringWithCancelExistingCancelsEverythingBeforeRebuilding() = runTest {
        givenTask(1L, "one")
        givenTask(2L, "two")
        whenever(notificationDao.getAllOrdered()) doReturn listOf(notification(1L), notification(2L))

        notificationManager.restoreNotifications(cancelExisting = true)

        assertEquals(
            listOf("cancel:1", "cancel:2", "notify:1", "notify:2", "cancel:0"),
            shade,
        )
    }

    @Test
    fun restoringWithoutCancelExistingRepostsOverWhatIsThere() = runTest {
        givenTask(1L, "one")
        whenever(notificationDao.getAllOrdered()) doReturn listOf(notification(1L))

        notificationManager.restoreNotifications(cancelExisting = false)

        assertEquals(listOf("notify:1", "cancel:0"), shade)
    }

    @Test
    fun restoringWithBundlingBuildsTheSummaryFirst() = runTest {
        givenTask(1L, "one")
        givenTask(2L, "two")
        whenever(preferences.bundleNotifications()) doReturn true
        whenever(notificationDao.getAllOrdered()) doReturn listOf(notification(1L), notification(2L))

        notificationManager.restoreNotifications(cancelExisting = false)

        assertEquals(listOf("notify:0", "notify:1", "notify:2"), shade)
    }

    @Test
    fun restoringDoesNothingWithoutPermission() = runTest {
        whenever(permissionChecker.canNotify()) doReturn false
        whenever(notificationDao.getAllOrdered()) doReturn listOf(notification(1L))

        notificationManager.restoreNotifications(cancelExisting = true)

        assertEquals(emptyList<String>(), shade)
    }

    @Test
    fun cancellingTheSummaryTakesEverythingWithIt() = runTest {
        whenever(notificationDao.getAll()) doReturn listOf(1L, 2L)

        notificationManager.cancel(
            NotificationManager.SUMMARY_NOTIFICATION_ID.toLong(),
            CancelReason.DISMISS,
        )

        assertEquals(listOf("cancel:1", "cancel:2", "cancel:0", "cancel:0"), shade)
        verifyBlocking(notificationDao) { deleteAll(listOf(1L, 2L, 0L)) }
    }

    @Test
    fun theNotificationCarriesTheTasksTitle() = runTest {
        givenTask(1L, "water the plants")

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        val posted = built.getValue(1)
        assertEquals(
            "water the plants",
            posted.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
        )
        assertEquals(
            listOf(getString(Res.string.rmd_NoA_done), getString(Res.string.rmd_NoA_snooze)),
            posted.actions.orEmpty().map { it.title.toString() },
        )
    }

    @Test
    fun aReadOnlyTaskIsNotOfferedComplete() = runTest {
        givenTask(1L, "someone else's", readOnly = true)

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        assertEquals(
            listOf(getString(Res.string.rmd_NoA_snooze)),
            built.getValue(1).actions.orEmpty().map { it.title.toString() },
        )
    }

    @Test
    fun theWearableExtenderCarriesSnoozeOptions() = runTest {
        givenTask(1L, "one")

        notificationManager.notifyTasks(listOf(notification(1L)), alert = true, nonstop = false, fiveTimes = false)

        val wearable = NotificationCompat.WearableExtender(built.getValue(1))
        assertTrue(wearable.actions.size > 1)
        assertTrue(wearable.actions.all { !it.title.isNullOrBlank() })
    }

    private fun alertBehaviour(id: Int) =
        NotificationCompat.getGroupAlertBehavior(built.getValue(id))

    private fun givenTask(
        id: Long,
        title: String,
        completionDate: Long = 0L,
        readOnly: Boolean = false,
    ) {
        tasks[id] = Task(
            id = id,
            title = title,
            completionDate = completionDate,
            readOnly = readOnly,
        )
    }

    private fun notification(taskId: Long, timestamp: Long = TIMESTAMP) = Notification(
        taskId = taskId,
        timestamp = timestamp,
        type = Alarm.TYPE_DATE_TIME,
    )

    private object PassThroughMarkdown : Markdown {
        override fun textWatcher(editText: android.widget.EditText) = null
        override val enabled = false
        override fun setMarkdown(tv: android.widget.TextView, markdown: String?) = Unit
        override fun toMarkdown(markdown: String?): CharSequence? = markdown
    }

    private companion object {
        const val TIMESTAMP = 1_754_000_000_000L
    }
}
