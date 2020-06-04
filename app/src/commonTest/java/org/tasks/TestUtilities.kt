package org.tasks

import android.content.Context
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.todoroo.astrid.data.Task
import org.tasks.caldav.CaldavConverter
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

    private fun fromResource(path: String): at.bitfire.ical4android.Task {
        val url = javaClass.classLoader!!.getResource(path)
        val paths = Paths.get(url!!.toURI())
        return fromString(String(Files.readAllBytes(paths), Charsets.UTF_8))
    }

    private fun fromString(task: String) = tasksFromReader(StringReader(task))[0]
}