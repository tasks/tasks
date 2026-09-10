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
    val current = unique.associateWith { taskRow(it)?.tagIds.orEmpty().toSet() }
    return transaction {
        unique.map { writer.editTaskTags(it, current.getValue(it), adds, removes) }
    }
}
