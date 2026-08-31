package org.tasks.notifications

import org.tasks.time.monotonicMillis

internal class LinuxNotificationIds(
    private val elapsedRealtime: () -> Long = { monotonicMillis() },
) {
    private val lock = Any()
    private val taskToId = mutableMapOf<Long, Int>()
    private val idToTask = mutableMapOf<Int, Long>()

    private val actedOn = mutableMapOf<Int, Long>()

    private var highWater = 0

    fun idFor(taskId: Long): Int? = synchronized(lock) { taskToId[taskId] }

    fun posted(taskId: Long, id: Int) = synchronized(lock) {
        forget(taskId)

        idToTask.remove(id)?.let { taskToId.remove(it) }
        taskToId[taskId] = id
        idToTask[id] = taskId
    }

    fun actionInvoked(id: Int): Long? = synchronized(lock) {
        idToTask[id]?.also {
            val now = elapsedRealtime()

            actedOn.values.removeAll { at -> now - at > ACTED_WINDOW_MS }
            actedOn[id] = now
        }
    }

    fun closed(id: Int): Closed = synchronized(lock) {
        val acted = actedOn.remove(id)?.let { elapsedRealtime() - it <= ACTED_WINDOW_MS } == true
        Closed(taskId = forgetId(id), acted = acted)
    }

    data class Closed(val taskId: Long?, val acted: Boolean)

    fun dismissing(taskIds: List<Long>): Map<Long, Int> = synchronized(lock) {
        taskIds
            .mapNotNull { taskId -> taskToId[taskId]?.let { taskId to it } }
            .onEach { (_, id) -> forgetId(id) }
            .toMap()
    }

    fun adopt(ids: Map<Long, Int>) = synchronized(lock) {
        ids.forEach { (taskId, id) ->
            if (!taskToId.containsKey(taskId) && !idToTask.containsKey(id)) {
                taskToId[taskId] = id
                idToTask[id] = taskId
            }
        }
        highWater = maxOf(highWater, ids.values.maxOrNull() ?: 0)
    }

    fun counterWentBackwards(id: Int): Boolean = synchronized(lock) {
        val wentBackwards = id <= highWater
        highWater = id
        wentBackwards
    }

    fun serverRestarted(): List<Long> = synchronized(lock) {
        val tasks = taskToId.keys.toList()
        taskToId.clear()
        idToTask.clear()
        actedOn.clear()
        tasks
    }

    fun takeAll(): List<Int> = synchronized(lock) {
        val live = idToTask.keys.toList()
        taskToId.clear()
        idToTask.clear()
        actedOn.clear()
        live
    }

    private fun forgetId(id: Int): Long? = idToTask.remove(id)?.also {
        taskToId.remove(it)
        actedOn.remove(id)
    }

    private fun forget(taskId: Long) {
        taskToId.remove(taskId)?.let {
            idToTask.remove(it)
            actedOn.remove(it)
        }
    }

    companion object {
        internal const val ACTED_WINDOW_MS = 10_000L
    }
}
