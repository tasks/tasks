package org.tasks.data

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.CancellationException
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.SYNC_ALARMS
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis

data class SubtaskWriteResult(
    val wrote: Boolean,
    val staged: List<SubtaskNode>,
    val read: List<TaskContainer>? = null,
) {
    companion object {
        val Nothing = SubtaskWriteResult(wrote = false, staged = emptyList())
    }
}

class SubtaskTreeWriter(
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val googleTaskDao: GoogleTaskDao,
    private val tagDao: TagDao,
    private val dirtyDao: DirtyDao,
    private val alarmService: AlarmService,
    private val taskCompleter: TaskCompleter,
    private val taskDeleter: TaskDeleter,
    private val taskSaver: TaskSaver,
    private val refreshBroadcaster: RefreshBroadcaster,
) {
    private val log = Logger.withTag("SubtaskTreeWriter")

    private class Created(val task: Task, val tags: List<TagData>, val reopens: Long?)

    private class Plan(
        val current: List<TaskContainer>,
        val rows: List<SubtaskRow>,
        val live: List<SubtaskRow>,
        val doomedIds: Set<Long>,
        val arranged: Map<Long, List<Long>>,
        val emptied: Set<Long>,
    )

    private class Placement(
        val created: Map<String, Created>,
        val ids: Map<String, Long>,
        val runs: Map<Long, List<Long>>,
        val touched: Set<Long>,
        val wrote: Boolean,
    )

    private class Committed(
        val plan: Plan,
        val placement: Placement,
        val wrote: Boolean,
    )

    suspend fun write(
        trees: SubtaskTrees,
        rootKey: String,
        parent: Task,
        list: CaldavFilter,
        untitled: String,
    ): SubtaskWriteResult {
        if (trees.rowsOf(rootKey).isEmpty()) {
            return SubtaskWriteResult.Nothing
        }
        val singleLevel = list.isSingleLevel
        val committed = taskDao.inTransaction {
            val plan = plan(trees, rootKey, parent, list)
            val placement = createRows(plan, parent, list, untitled, singleLevel)
            val positioned = positionRuns(plan, placement, list)
            Committed(plan = plan, placement = placement, wrote = placement.wrote || positioned)
        }
        val plan = committed.plan
        val created = committed.placement.created
        val applied = mutableMapOf<String, SubtaskNode>()
        val failures = mutableListOf<Throwable>()
        val completed: Boolean
        val settled: Map<String, SubtaskNode>
        try {
            completed = afterCommit(
                trees = trees,
                parent = parent,
                live = plan.live,
                ids = committed.placement.ids,
                created = created,
                doomedIds = plan.doomedIds,
                applied = applied,
                failures = failures,
                untitled = untitled,
            ) || committed.wrote
        } finally {
            settled = trees.settle(created.mapValues { it.value.task }, applied)
        }
        if (completed) {
            log.d { "wrote ${plan.rows.size} subtasks under ${parent.id}" }
            refreshBroadcaster.broadcastRefresh()
        }
        failures.firstOrNull()?.let { throw it }
        return SubtaskWriteResult(
            wrote = completed,
            staged = plan.rows.map { settled[it.key] ?: it.node },
            read = plan.current,
        )
    }

    private suspend fun plan(
        trees: SubtaskTrees,
        rootKey: String,
        parent: Task,
        list: CaldavFilter,
    ): Plan {
        val current = taskDao
            .fetchTasks(subtaskQuery(parentId = parent.id, isGoogleTasks = list.isGoogleTasks))
        trees.merge(rootKey, parent.id, current)
        val rows = trees.rowsOf(rootKey)
        val doomed = rows.doomed()
        val doomedRows = rows.filter { it.key in doomed }
        val doomedIds = doomedRows.mapNotNull { it.node.id.takeIf { id -> id > 0 } }.toSet()
        return Plan(
            current = current,
            rows = rows,
            live = rows.filterNot { it.key in doomed },
            doomedIds = doomedIds,
            arranged = current
                .filterNot { it.id in doomedIds }
                .groupBy({ it.task.parent }, { it.id }),
            emptied = doomedRows
                .filter { it.node.id > 0 }
                .mapTo(mutableSetOf()) { it.node.task.parent },
        )
    }

    private suspend fun createRows(
        plan: Plan,
        parent: Task,
        list: CaldavFilter,
        untitled: String,
        singleLevel: Boolean,
    ): Placement {
        val created = linkedMapOf<String, Created>()
        val ids = mutableMapOf<String, Long>()
        val runs = linkedMapOf<Long, MutableList<Long>>()
        val touched = plan.emptied.toMutableSet()
        val remoteParents = mutableMapOf<Long, String?>()
        val kept = plan.live.keptBy(singleLevel)
        var wrote = false
        plan.live.forEach { row ->
            val node = row.node
            val parentId = if (singleLevel) parent.id else ids[node.parentKey] ?: parent.id
            val id = if (node.isNew) {
                create(
                    node = node,
                    parentId = parentId,
                    list = list,
                    untitled = untitled,
                    keepUntitled = row.key in kept,
                    remoteParents = remoteParents,
                )?.also {
                    created[node.key] = Created(
                        task = it,
                        tags = node.pending?.tags.orEmpty(),
                        reopens = parentId.takeUnless { row.completed },
                    )
                    wrote = true
                }?.id
            } else {
                node.id.takeIf { it > 0 }
            }
            if (id != null) {
                ids[node.key] = id
                runs.getOrPut(parentId) { mutableListOf() }.add(id)
                if (node.isNew || node.moved) {
                    touched.add(parentId)
                }
                if (!node.isNew && node.task.parent != parentId) {
                    touched.add(parentId)
                    touched.add(node.task.parent)
                }
            }
        }
        return Placement(
            created = created,
            ids = ids,
            runs = runs,
            touched = touched,
            wrote = wrote,
        )
    }

    private suspend fun positionRuns(
        plan: Plan,
        placement: Placement,
        list: CaldavFilter,
    ): Boolean {
        val createdIds = placement.created.values.mapTo(HashSet()) { it.task.id }
        val byId = taskDao
            .fetch(placement.ids.values.filterNot { it in createdIds })
            .associateByTo(mutableMapOf()) { it.id }
        placement.created.values.forEach { byId[it.task.id] = it.task }
        var wrote = false
        placement.runs.forEach { (parentId, children) ->
            if (parentId in placement.touched && children != plan.arranged[parentId]) {
                if (writeRun(parentId, children, list, byId)) {
                    wrote = true
                }
            }
        }
        return wrote
    }

    private suspend fun afterCommit(
        trees: SubtaskTrees,
        parent: Task,
        live: List<SubtaskRow>,
        ids: Map<String, Long>,
        created: Map<String, Created>,
        doomedIds: Set<Long>,
        applied: MutableMap<String, SubtaskNode>,
        failures: MutableList<Throwable>,
        untitled: String,
    ): Boolean {
        var wrote = false
        created.forEach { (key, row) ->
            failures.catching("create $key") {
                val pending = trees.takePending(key)
                val alarms = pending?.alarms?.applicableTo(row.task).orEmpty().toSet()
                if (alarms.isNotEmpty()) {
                    alarmService.synchronizeAlarms(row.task.id, alarms.toMutableSet())
                    row.task.putTransitory(SYNC_ALARMS, true)
                }
                val tags = pending?.tags.orEmpty()
                if (tags != row.tags) {
                    tagDao.applyTags(row.task, tags)
                    row.task.putTransitory(SYNC_TAGS, true)
                }
                row.task.modificationDate = currentTimeMillis()
                taskSaver.save(row.task, null)
                wrote = true
            }
        }
        created.values
            .mapNotNull { it.reopens }
            .distinct()
            .forEach { parentId ->
                failures.catching("reopen $parentId") { uncomplete(parentId, parent) }
            }
        live.forEach { row ->
            ids[row.key]?.let { id ->
                failures.catching("edits ${row.key}") {
                    if (applyEdits(trees, row.node, id, row.key in created, applied, untitled)) {
                        wrote = true
                    }
                }
            }
        }
        if (doomedIds.isNotEmpty()) {
            failures.catching("delete $doomedIds") {
                taskDeleter.markDeleted(doomedIds.toList())
                wrote = true
            }
        }
        return wrote
    }

    private suspend fun MutableList<Throwable>.catching(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed to $what after the subtask write committed" }
            add(e)
        }
    }

    private suspend fun create(
        node: SubtaskNode,
        parentId: Long,
        list: CaldavFilter,
        untitled: String,
        keepUntitled: Boolean,
        remoteParents: MutableMap<Long, String?>,
    ): Task? {
        val pending = node.pending ?: return null
        val task = node.task.copy(title = node.title)
        if (task.title.isNullOrBlank()) {
            if (!keepUntitled) {
                return null
            }
            task.title = untitled
        }
        task.parent = parentId
        taskDao.createNew(task)
        if (list.isGoogleTasks) {
            googleTaskDao.insertAndShift(
                task = task,
                caldavTask = CaldavTask(
                    task = task.id,
                    calendar = list.uuid,
                    remoteId = null,
                    isMoved = true,
                ),
                top = false,
            )
        } else {
            caldavDao.insert(
                task = task,
                caldavTask = CaldavTask(
                    task = task.id,
                    calendar = list.uuid,
                    remoteParent = if (remoteParents.containsKey(parentId)) {
                        remoteParents[parentId]
                    } else {
                        val remoteId = if (list.account.pushesRemoteParent) {
                            caldavDao.getRemoteIdForTask(parentId)
                        } else {
                            null
                        }
                        remoteParents[parentId] = remoteId
                        remoteId
                    },
                ),
                addToTop = false,
            )
        }
        if (pending.tags.isNotEmpty()) {
            tagDao.applyTags(task, pending.tags)
            task.putTransitory(SYNC_TAGS, true)
        }
        return task
    }

    private suspend fun uncomplete(parentId: Long, parent: Task) {
        val row = if (parentId == parent.id) parent else taskDao.fetch(parentId) ?: return
        if (!row.isCompleted) {
            return
        }
        taskCompleter.setComplete(row, completed = false, includeChildren = false)
    }

    private suspend fun writeRun(
        parentId: Long,
        children: List<Long>,
        list: CaldavFilter,
        byId: MutableMap<Long, Task>,
    ): Boolean {
        val reparented = mutableListOf<Long>()
        val dirty = mutableListOf<Long>()
        val order = if (list.isGoogleTasks) {
            emptyList()
        } else {
            sortKeysFor(children.map { byId[it] ?: taskDao.fetch(it) }, parentId)
        }
        children.forEachIndexed { position, id ->
            if (list.isGoogleTasks) {
                val row = taskDao.fetch(id) ?: return@forEachIndexed
                val unpositioned = row.order == null
                if (unpositioned) {
                    taskDao.setOrder(id, position.toLong())
                    row.order = position.toLong()
                }
                if (unpositioned || row.parent != parentId || row.order != position.toLong()) {
                    googleTaskDao.move(
                        task = row,
                        list = list.uuid,
                        newParent = parentId,
                        newPosition = position.toLong(),
                    )
                    dirty.add(id)
                }
                byId[id]?.let { it.parent = row.parent; it.order = row.order }
                return@forEachIndexed
            }
            val row = byId[id] ?: taskDao.fetch(id) ?: return@forEachIndexed
            val target = order[position]
            val reparent = row.parent != parentId
            if (!reparent && row.order == target) {
                return@forEachIndexed
            }
            if (row.order != target) {
                taskDao.setOrder(id, target)
                row.order = target
            }
            if (reparent) {
                reparented.add(id)
                row.parent = parentId
            }
            dirty.add(id)
        }
        if (reparented.isNotEmpty()) {
            taskDao.setParent(parentId, reparented)
            caldavDao.setRemoteParent(reparented, parentId, list.account, list.uuid)
        }
        if (dirty.isNotEmpty()) {
            dirtyDao.setDirty(dirty)
        }
        return dirty.isNotEmpty() || reparented.isNotEmpty()
    }

    private suspend fun applyEdits(
        trees: SubtaskTrees,
        node: SubtaskNode,
        id: Long,
        titleWritten: Boolean,
        applied: MutableMap<String, SubtaskNode>,
        untitled: String,
    ): Boolean {
        val staged = trees.get(node.key) ?: return false
        val writesTitle = staged.titleEdited && !titleWritten
        val writesPending = staged.pendingUnwritten && !titleWritten
        var wroteTitle = false
        var wroteTick = false
        try {
            if (!writesTitle && !staged.completionEdited && !writesPending) {
                return false
            }
            var row = taskDao.fetch(id) ?: return false
            var wrote = false
            if (writesPending) {
                trees.takePending(node.key)?.let { pending ->
                    val alarms = pending.alarms.applicableTo(row).toSet()
                    if (alarms.isNotEmpty()) {
                        alarmService.synchronizeAlarms(row.id, alarms.toMutableSet())
                        row.putTransitory(SYNC_ALARMS, true)
                        wrote = true
                    }
                    if (pending.tags.isNotEmpty()) {
                        tagDao.applyTags(row, pending.tags)
                        row.putTransitory(SYNC_TAGS, true)
                        wrote = true
                    }
                }
            }
            if (writesTitle) {
                val title = staged.title?.takeUnless { it.isBlank() } ?: untitled
                if (title != row.title) {
                    val edited = row.copy(title = title)
                    taskSaver.save(edited, row)
                    row = edited
                    wrote = true
                }
                wroteTitle = true
            }
            if (staged.completionEdited) {
                if (row.isCompleted != staged.completed) {
                    taskCompleter.setComplete(row, staged.completed)
                    wrote = true
                }
                wroteTick = true
            }
            return wrote
        } finally {
            applied[node.key] = staged.copy(
                stagedTitle = staged.stagedTitle.takeIf { wroteTitle || !writesTitle },
                stagedCompleted = staged.stagedCompleted.takeIf {
                    wroteTick || !staged.completionEdited
                },
            )
        }
    }
}

private fun List<SubtaskRow>.keptBy(singleLevel: Boolean): Set<String> =
    (if (singleLevel) map { it.copy(indent = 0) } else this).survivors()

private fun List<SubtaskRow>.survivors(): Set<String> {
    val kept = HashSet<String>()
    for (index in indices.reversed()) {
        val row = this[index]
        val survives = !row.node.isNew ||
                !row.node.title.isNullOrBlank() ||
                row.node.holdsContent ||
                subList(index + 1, size)
                    .takeWhile { it.indent > row.indent }
                    .any { it.key in kept }
        if (survives) {
            kept.add(row.key)
        }
    }
    return kept
}

private val SubtaskNode.holdsContent: Boolean
    get() {
        val pending = pending ?: return false
        return pending.tags.isNotEmpty() ||
                pending.startDay != NO_DAY ||
                pending.startTime != NO_TIME ||
                completed ||
                !task.notes.isNullOrBlank() ||
                task.dueDate > 0 ||
                task.hideUntil > 0 ||
                task.priority != Task.Priority.NONE ||
                task.isRecurring ||
                task.estimatedSeconds > 0 ||
                task.elapsedSeconds > 0
    }
