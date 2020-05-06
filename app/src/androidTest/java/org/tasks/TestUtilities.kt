package org.tasks

import android.content.Context
import androidx.test.InstrumentationRegistry
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.todoroo.astrid.data.Task
import org.tasks.caldav.CaldavConverter
import org.tasks.preferences.Preferences
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

object TestUtilities {
    private var mockitoInitialized = false
    @JvmStatic
    fun initializeMockito(context: Context) {
        if (!mockitoInitialized) {
            // for mockito: https://code.google.com/p/dexmaker/issues/detail?id=2
            System.setProperty("dexmaker.dexcache", context.cacheDir.toString())
            mockitoInitialized = true
        }
    }

    @JvmStatic
    fun newPreferences(context: Context?): Preferences {
        return Preferences(context, "test_preferences")
    }

    @JvmStatic
    fun vtodo(path: String): Task {
        val task = Task()
        CaldavConverter.apply(task, fromResource(path))
        return task
    }

    private fun fromResource(path: String): at.bitfire.ical4android.Task {
        val context = InstrumentationRegistry.getInstrumentation().context
        var `is`: InputStream? = null
        var reader: InputStreamReader? = null
        return try {
            `is` = context.assets.open(path)
            reader = InputStreamReader(`is`, Charsets.UTF_8)
            fromString(reader.readText())
        } catch (e: IOException) {
            throw IllegalArgumentException(e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (ignored: IOException) {
                }
            }
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    private fun fromString(task: String) = try {
        tasksFromReader(StringReader(task))[0]
    } catch (e: Exception) {
        throw IllegalArgumentException(e)
    }
}