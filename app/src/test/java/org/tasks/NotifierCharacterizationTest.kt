package org.tasks

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.todoroo.astrid.voice.VoiceOutputAssistant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.AudioManager
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences

@RunWith(RobolectricTestRunner::class)
class NotifierCharacterizationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val taskDao: TaskDao = mock()
    private val notificationManager: NotificationManager = mock()
    private val telephonyManager: TelephonyManager = mock()
    private val audioManager: AudioManager = mock()
    private val voiceOutputAssistant: VoiceOutputAssistant = mock()
    private val preferences: Preferences = mock()

    private val tasks = mutableMapOf<Long, Task>()

    private lateinit var notifier: Notifier

    @Before
    fun setUp() {
        taskDao.stub {
            onBlocking { fetch(any<Long>()) } doAnswer { tasks[it.arguments[0] as Long] }
        }
        preferences.stub {
            on { getBoolean(any<Int>(), any()) } doReturn false
        }
        notifier = Notifier(
            context = context,
            taskDao = taskDao,
            notificationManager = notificationManager,
            telephonyManager = telephonyManager,
            audioManager = audioManager,
            voiceOutputAssistant = voiceOutputAssistant,
            preferences = preferences,
        )
    }

    @Test
    fun tasksAreReportedWhenNothingCouldBeBuilt() = runTest {
        givenTask(1L)
        givenTask(2L)
        notificationManager.stub {
            onBlocking { getTaskNotification(any()) } doReturn null
        }

        val reported = notifier.triggerNotifications(listOf(notification(1L), notification(2L)))

        assertEquals(listOf(1L, 2L), reported.toList())
    }

    @Test
    fun aTaskIsReportedEvenWhenNotificationManagerDeliveredNoneOfIt() = runTest {
        givenTask(1L)
        givenTask(2L)
        val builder = NotificationCompat.Builder(context, "channel")
        notificationManager.stub {
            onBlocking { getTaskNotification(any()) } doReturn builder
            onBlocking { notifyTasks(any(), any(), any(), any()) } doReturn emptyList()
        }

        val reported = notifier.triggerNotifications(listOf(notification(1L), notification(2L)))

        assertEquals(listOf(1L, 2L), reported.toList())
    }

    private fun givenTask(id: Long) {
        tasks[id] = Task(id = id, title = "task $id")
    }

    private fun notification(taskId: Long) = Notification(
        taskId = taskId,
        timestamp = 1_754_000_000_000L,
        type = Alarm.TYPE_DATE_TIME,
    )
}
