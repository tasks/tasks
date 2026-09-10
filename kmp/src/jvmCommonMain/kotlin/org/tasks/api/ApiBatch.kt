package org.tasks.api

import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags

data class TaskUpdate(
    val id: Long,
    val patch: TaskWrite,
)

fun requireChanges(updates: List<TaskUpdate>) {
    updates.firstOrNull { it.patch.isEmpty }?.let {
        throw IllegalArgumentException(
            "The entry for task ${it.id} has no fields to change - send at least one."
        )
    }
}

data class Creation(
    val ids: List<Long>,
    val tasks: List<TaskRow>,
    val movedToParentListIds: List<Long>,
)

suspend fun ApiQueryEngine.createTasks(writer: ApiWriter, tasks: List<TaskWrite>): Creation {
    requireBatch(tasks.size)
    val ids = transaction {
        tasks.mapIndexed { index, task ->
            entry(index, tasks.size, task.title) { writer.insertTask(task.toValues()) }
        }
    }
    val created = tasksById(ids)
    val landedOn = created.associate { it.id to it.listId }
    return Creation(
        ids = ids,
        tasks = created,
        movedToParentListIds = tasks.zip(ids)
            .filter { (write, id) -> movedToParentList(write.parentId, write.listId, landedOn[id]) }
            .map { it.second },
    )
}

private inline fun <T> entry(index: Int, of: Int, label: String?, block: () -> T): T = try {
    block()
} catch (e: IllegalArgumentException) {
    if (of == 1) throw e
    throw IllegalArgumentException("Entry ${index + 1} of $of (${label ?: "untitled"}): ${e.message}", e)
}

data class Revision(
    val rowsChanged: List<Int>,
    val tasks: List<TaskRow>,
    val unchangedIds: List<Long>,
    val movedToParentListIds: List<Long>,
    val detachedIds: List<Long>,
)

suspend fun ApiQueryEngine.updateTasks(writer: ApiWriter, updates: List<TaskUpdate>): Revision {
    requireBatch(updates.size, "updates")
    requireDistinct(updates.map { it.id })
    requireChanges(updates)
    val parentsBefore = tasksById(updates.map { it.id }).associate { it.id to it.parentId }
    val changed = transaction {
        updates.mapIndexed { index, update ->
            entry(index, updates.size, "task ${update.id}") { writer.updateTask(update.id, update.patch.toValues()) }
        }
    }
    val applied = updates.zip(changed).filter { it.second > 0 }.map { it.first }
    val after = tasksById(applied.map { it.id })
    val landedOn = after.associate { it.id to it.listId }
    val parentsAfter = after.associate { it.id to it.parentId }
    return Revision(
        rowsChanged = changed,
        tasks = after,
        unchangedIds = updates.zip(changed).filter { it.second == 0 }.map { it.first.id },
        movedToParentListIds = applied
            .filter { movedToParentList(it.patch.parentId, it.patch.listId, landedOn[it.id]) }
            .map { it.id },
        detachedIds = applied
            .filter { it.patch.parentId == null && parentsBefore[it.id] != null && parentsAfter[it.id] == null }
            .map { it.id },
    )
}

suspend fun ApiQueryEngine.completeTasks(
    writer: ApiWriter,
    ids: List<Long>,
    completed: Boolean,
    completedAt: Any? = null,
): Completion {
    val unique = ids.distinct()
    requireBatch(unique.size)
    val stamp = if (completed) completedAt ?: System.currentTimeMillis() else 0L
    val values = ApiValues.of(TasksContract.Tasks.COMPLETED_AT to stamp)
    val before = unique.associateWith { taskRow(it)?.recurrence }
    val related = (writer.descendantsOf(unique) + unique.flatMap { writer.ancestorsOf(it) })
        .distinct()
        .filter { it !in unique }
    val relatedBefore = tasksById(related).associate { it.id to (it.completed != null) }
    val changed = transaction { unique.map { writer.updateTask(it, values) } }
    val tasks = tasksById(unique)
    val byId = tasks.associateBy { it.id }
    val after = unique.associateWith { byId[it]?.completed }
    val relatedAfter = tasksById(related).associate { it.id to (it.completed != null) }
    return Completion(
        taskIds = unique,
        rowsChanged = changed,
        advancedTaskIds = if (completed) advancedSeries(before, after) else emptyList(),
        alsoCompletedTaskIds = related.filter { relatedBefore[it] == false && relatedAfter[it] == true },
        reopenedTaskIds = related.filter { relatedBefore[it] == true && relatedAfter[it] == false },
        tasks = tasks,
    )
}

data class TagChange(
    val edits: List<TagEdit>,
    val tasks: List<TaskRow>,
) {
    val added: Int get() = edits.sumOf { it.added }

    val removed: Int get() = edits.sumOf { it.removed }
}

suspend fun ApiQueryEngine.setTaskTags(
    writer: ApiWriter,
    taskIds: List<Long>,
    add: List<Long>,
    remove: List<Long>,
): TagChange {
    val unique = taskIds.distinct()
    requireBatch(unique.size)
    val adds = add.distinct()
    val removes = remove.distinct()
    require(adds.isNotEmpty() || removes.isNotEmpty()) {
        "Nothing to change - send tags to add, tags to remove, or both."
    }
    val current = unique.associateWith { taskRow(it)?.tagIds.orEmpty().toSet() }
    val edits = transaction {
        unique.map { writer.editTaskTags(it, current.getValue(it), adds, removes) }
    }
    return TagChange(edits, tasksById(unique))
}

data class Deletion(
    val rowsDeleted: Int,
    val alsoAffected: Int,
)

suspend fun ApiQueryEngine.deleteTask(writer: ApiWriter, id: Long): Deletion {
    val subtasks = writer.descendantsOf(listOf(id)).size
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

suspend fun ApiQueryEngine.createList(writer: ApiWriter, write: ListWrite): ListRow =
    row(Lists.PATH, writer.insertList(write.toValues())) { it.toListRow() }

suspend fun ApiQueryEngine.changeList(writer: ApiWriter, id: Long, write: ListWrite): ListRow {
    writer.updateList(id, write.toValues()).orNotFound(Lists.PATH, id)
    return row(Lists.PATH, id) { it.toListRow() }
}

data class Created<T>(val row: T, val created: Boolean)

suspend fun ApiQueryEngine.createTag(writer: ApiWriter, write: TagWrite): Created<TagRow> {
    val insertion = writer.insertTag(write.toValues())
    return Created(row(Tags.PATH, insertion.id) { it.toTagRow() }, insertion.created)
}

suspend fun ApiQueryEngine.changeTag(writer: ApiWriter, id: Long, write: TagWrite): TagRow {
    writer.updateTag(id, write.toValues()).orNotFound(Tags.PATH, id)
    return row(Tags.PATH, id) { it.toTagRow() }
}

suspend fun ApiQueryEngine.createPlace(writer: ApiWriter, write: PlaceWrite): Created<PlaceRow> {
    val insertion = writer.insertPlace(write.toValues())
    return Created(row(Places.PATH, insertion.id) { it.toPlaceRow() }, insertion.created)
}

suspend fun ApiQueryEngine.changePlace(writer: ApiWriter, id: Long, write: PlaceWrite): PlaceRow {
    writer.updatePlace(id, write.toValues()).orNotFound(Places.PATH, id)
    return row(Places.PATH, id) { it.toPlaceRow() }
}

private fun Int.orNotFound(path: String, id: Long) {
    if (this == 0) throw ApiRowNotFound(path, id)
}

private suspend fun <T> ApiQueryEngine.row(path: String, id: Long, map: (ApiRow) -> T): T =
    queryById(path, id).firstOrNull()?.let(map) ?: throw ApiRowNotFound(path, id)

data class ReminderEdit(
    val addedIds: List<Long>,
    val removed: Int,
    val reminders: List<ReminderRow>,
    val task: TaskRow?,
)

suspend fun ApiQueryEngine.setTaskReminders(
    writer: ApiWriter,
    taskId: Long,
    add: List<ReminderWrite>,
    removeReminderIds: List<Long>,
): ReminderEdit {
    val removals = removeReminderIds.distinct()
    val edit = transaction {
        add.map { writer.insertReminder(it.toValues(taskId)) } to
            removals.sumOf { writer.deleteReminder(it) }
    }
    return ReminderEdit(
        addedIds = edit.first.distinct(),
        removed = edit.second,
        reminders = findReminders(ReminderQuery(taskIds = listOf(taskId))).rows,
        task = taskRow(taskId),
    )
}
