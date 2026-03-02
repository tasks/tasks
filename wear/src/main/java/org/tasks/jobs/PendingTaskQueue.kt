package org.tasks.jobs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PendingTask(val title: String, val filter: String?)

class PendingTaskQueue private constructor(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun add(title: String, filter: String?) {
        val tasks = getAll().toMutableList()
        tasks.add(PendingTask(title, filter))
        save(tasks)
    }

    @Synchronized
    fun getAll(): List<PendingTask> {
        val json = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PendingTask(
                    title = obj.getString("title"),
                    filter = if (obj.has("filter")) obj.getString("filter") else null,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun remove(task: PendingTask) {
        val tasks = getAll().toMutableList()
        tasks.remove(task)
        save(tasks)
    }

    private fun save(tasks: List<PendingTask>) {
        val array = JSONArray()
        for (task in tasks) {
            array.put(JSONObject().apply {
                put("title", task.title)
                task.filter?.let { put("filter", it) }
            })
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "pending_tasks"
        private const val KEY_TASKS = "tasks"

        @Volatile
        private var instance: PendingTaskQueue? = null

        fun getInstance(context: Context): PendingTaskQueue =
            instance ?: synchronized(this) {
                instance ?: PendingTaskQueue(context).also { instance = it }
            }
    }
}
