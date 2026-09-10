package org.tasks.api

data class TaskUpdate(
    val id: Long,
    val patch: TaskWrite,
)

suspend fun ApiQueryEngine.createTasks(writer: ApiWriter, tasks: List<TaskWrite>): List<Long> {
    requireBatch(tasks.size)
    return transaction { tasks.map { writer.insertTask(it.toValues()) } }
}

suspend fun ApiQueryEngine.updateTasks(writer: ApiWriter, updates: List<TaskUpdate>): List<Int> {
    requireBatch(updates.size, "updates")
    requireDistinct(updates.map { it.id })
    return transaction { updates.map { writer.updateTask(it.id, it.patch.toValues()) } }
}

suspend fun ApiQueryEngine.completeTasks(
    writer: ApiWriter,
    ids: List<Long>,
    completed: Boolean,
    completedAt: Long? = null,
): Completion {
    val unique = ids.distinct()
    requireBatch(unique.size)
    val stamp = if (completed) completedAt ?: System.currentTimeMillis() else 0L
    val values = ApiValues.of(TasksContract.Tasks.COMPLETED_AT to stamp)
    val before = unique.associateWith { taskRow(it)?.recurrence }
    val changed = transaction { unique.map { writer.updateTask(it, values) } }
    val after = unique.associateWith { taskRow(it)?.completed }
    return Completion(
        taskIds = unique,
        rowsChanged = changed,
        advancedTaskIds = if (completed) advancedSeries(before, after) else emptyList(),
    )
}

suspend fun ApiQueryEngine.setTaskTags(
    writer: ApiWriter,
    taskIds: List<Long>,
    add: List<Long>,
    remove: List<Long>,
): List<TagEdit> {
    val unique = taskIds.distinct()
    requireBatch(unique.size)
    val adds = add.distinct()
    val removes = remove.distinct()
    require(adds.isNotEmpty() || removes.isNotEmpty()) {
        "Nothing to change - send tags to add, tags to remove, or both."
    }
    val current = unique.associateWith { taskRow(it)?.tagIds.orEmpty().toSet() }
    return transaction {
        unique.map { writer.editTaskTags(it, current.getValue(it), adds, removes) }
    }
}

data class Deletion(
    val rowsDeleted: Int,
    val alsoAffected: Int,
)

suspend fun ApiQueryEngine.deleteTask(writer: ApiWriter, id: Long): Deletion {
    val subtasks = taskRow(id)?.childCount ?: 0
    return deletion(writer.deleteTask(id), subtasks)
}

suspend fun ApiQueryEngine.deleteList(writer: ApiWriter, id: Long): Deletion {
    val tasks = countTasks(TaskQuery(listIds = listOf(id), status = "any"))
    return deletion(writer.deleteList(id), tasks)
}

suspend fun ApiQueryEngine.deleteTag(writer: ApiWriter, id: Long): Deletion {
    val tagged = countTasks(TaskQuery(tagIds = listOf(id), status = "any"))
    return deletion(writer.deleteTag(id), tagged)
}

suspend fun ApiQueryEngine.deletePlace(writer: ApiWriter, id: Long): Deletion {
    val filed = countTasks(TaskQuery(placeIds = listOf(id), status = "any"))
    return deletion(writer.deletePlace(id), filed)
}

private fun deletion(rows: Int, alsoAffected: Int) =
    Deletion(rows, if (rows > 0) alsoAffected else 0)

data class ReminderEdit(
    val addedIds: List<Long>,
    val removed: Int,
    val reminders: List<ReminderRow>,
)

suspend fun ApiQueryEngine.setTaskReminders(
    writer: ApiWriter,
    taskId: Long,
    add: List<ReminderWrite>,
    removeReminderIds: List<Long>,
): ReminderEdit {
    val removals = removeReminderIds.distinct()
    val edit = transaction {
        add.map { writer.insertReminder(it.toValues()) } to removals.sumOf { writer.deleteReminder(it) }
    }
    return ReminderEdit(
        addedIds = edit.first,
        removed = edit.second,
        reminders = findReminders(ReminderQuery(taskIds = listOf(taskId))).rows,
    )
}
