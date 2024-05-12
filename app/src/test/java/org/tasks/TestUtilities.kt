package org.tasks

import android.content.Context
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.squareup.moshi.Moshi
import org.tasks.data.entity.Task
import kotlinx.coroutines.runBlocking
import org.tasks.caldav.applyRemote
import org.tasks.caldav.iCalendar.Companion.reminders
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavTask
import org.tasks.preferences.Preferences
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.sync.microsoft.Tasks
import org.tasks.time.DateTime
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.TimeZone

object TestUtilities {
    fun withTZ(id: String, runnable: suspend () -> Unit) =
        withTZ(TimeZone.getTimeZone(id), runnable)

    fun withTZ(tz: TimeZone, runnable: suspend () -> Unit) {
        val def = TimeZone.getDefault()
        try {
            TimeZone.setDefault(tz)
            runBlocking {
                runnable()
            }
        } finally {
            TimeZone.setDefault(def)
        }
    }

    fun assertEquals(expected: Long, actual: DateTime) =
        org.junit.Assert.assertEquals(expected, actual.millis)

    fun assertEquals(expected: DateTime, actual: Long?) =
        org.junit.Assert.assertEquals(expected.millis, actual)

    fun newPreferences(context: Context): Preferences {
        return Preferences(context, "test_preferences")
    }

    fun vtodo(path: String): Task {
        val task = Task()
        task.applyRemote(icalendarFromFile(path), null)
        return task
    }

    val String.alarms: List<Alarm>
        get() = icalendarFromFile(this).reminders

    fun setup(path: String): Triple<Task, CaldavTask, at.bitfire.ical4android.Task> {
        val task = Task()
        val remote = icalendarFromFile(path)
        task.applyRemote(remote, null)
        return Triple(task, CaldavTask(task = 0, calendar = null), remote)
    }

    fun icalendarFromFile(path: String): at.bitfire.ical4android.Task =
        tasksFromReader(StringReader(readFile(path)))
            .takeIf { it.size == 1 }
            ?.first()
            ?: throw IllegalStateException()

    fun mstodo(
        path: String,
        task: Task = Task(),
        defaultPriority: Int = Task.Priority.NONE,
    ): Pair<Task, Tasks.Task> {
        val remote = mstodoFromFile(path)
        task.applyRemote(remote, defaultPriority)
        return Pair(task, remote)
    }

    private fun mstodoFromFile(path: String): Tasks.Task =
        Moshi.Builder().build().adapter(Tasks::class.java).fromJson(readFile(path))!!.value.first()

    fun readFile(path: String): String {
        val uri = javaClass.classLoader?.getResource(path)?.toURI()
            ?: throw IllegalArgumentException()
        val paths = Paths.get(uri)
        return String(Files.readAllBytes(paths), Charsets.UTF_8)
    }
}