package org.tasks

import android.content.Context
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.todoroo.astrid.data.Task
import org.tasks.caldav.CaldavConverter
import org.tasks.data.CaldavTask
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object TestUtilities {
    fun withTZ(id: String, runnable: () -> Unit) = withTZ(TimeZone.getTimeZone(id), runnable)

    fun withTZ(tz: TimeZone, runnable: () -> Unit) {
        val def = TimeZone.getDefault()
        try {
            TimeZone.setDefault(tz)
            runnable()
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
        CaldavConverter.apply(task, fromResource(path))
        return task
    }

    fun setup(path: String): Triple<Task, CaldavTask, at.bitfire.ical4android.Task> {
        val task = Task()
        val vtodo = readFile(path)
        val remote = fromString(vtodo)
        CaldavConverter.apply(task, remote)
        return Triple(task, CaldavTask().apply { this.vtodo = vtodo }, remote)
    }

    private fun fromResource(path: String): at.bitfire.ical4android.Task =
            fromString(readFile(path))

    private fun readFile(path: String): String {
        val uri = javaClass.classLoader?.getResource(path)?.toURI()
                ?: throw IllegalArgumentException()
        val paths = Paths.get(uri)
        return String(Files.readAllBytes(paths), Charsets.UTF_8)
    }

    fun fromString(task: String): at.bitfire.ical4android.Task =
            tasksFromReader(StringReader(task))
                    .takeIf { it.size == 1 }
                    ?.first()
                    ?: throw IllegalStateException()
}