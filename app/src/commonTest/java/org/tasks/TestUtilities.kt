package org.tasks

import android.content.Context
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.todoroo.astrid.data.Task
import org.tasks.caldav.CaldavConverter
import org.tasks.data.CaldavTask
import org.tasks.preferences.Preferences
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths

object TestUtilities {
    fun newPreferences(context: Context): Preferences {
        return Preferences(context, "test_preferences")
    }

    fun vtodo(path: String): Task {
        val task = Task()
        CaldavConverter.apply(task, fromResource(path))
        return task
    }

    fun setup(path: String): Pair<Task, CaldavTask> {
        val task = Task()
        val vtodo = readFile(path)
        CaldavConverter.apply(task, fromString(vtodo))
        return Pair(task, CaldavTask().apply { this.vtodo = vtodo })
    }

    private fun fromResource(path: String): at.bitfire.ical4android.Task =
            fromString(readFile(path))

    fun readFile(path: String): String {
        val url = javaClass.classLoader!!.getResource(path)
        val paths = Paths.get(url!!.toURI())
        return String(Files.readAllBytes(paths), Charsets.UTF_8)
    }

    fun fromString(task: String): at.bitfire.ical4android.Task =
            task.let { tasksFromReader(StringReader(it))[0] }
}