package org.tasks.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter

class SubtaskTrees {
    private val _nodes = MutableStateFlow<Map<String, SubtaskNode>>(emptyMap())
    val nodes: StateFlow<Map<String, SubtaskNode>> = _nodes.asStateFlow()

    fun get(key: String?): SubtaskNode? = key?.let { _nodes.value[it] }

    fun holds(id: Long, remoteId: String?): Boolean = get(subtaskKey(id, remoteId)) != null

    fun isDoomed(id: Long, remoteId: String?): Boolean =
        _nodes.value.deletions().containsKey(subtaskKey(id, remoteId))

    fun rowsOf(rootKey: String): List<SubtaskRow> = _nodes.value.rowsOf(rootKey)

    fun clear() {
        _nodes.value = emptyMap()
    }

    fun isRearranged(rootKey: String): Boolean = _nodes.value.isRearranged(rootKey)

    fun merge(rootKey: String, rootId: Long, rows: List<TaskContainer>) {
        _nodes.update { it.merged(rootKey, rootId, rows) }
    }

    fun add(rootKey: String, task: Task, list: CaldavFilter?): SubtaskNode {
        val key = subtaskKey(task)
        var added = SubtaskNode(key = key, parentKey = rootKey, sequence = 0L, task = task)
        _nodes.update { nodes ->
            added = SubtaskNode(
                key = key,
                parentKey = rootKey,
                sequence = nodes.nextSequence(),
                task = task,
                pending = PendingTask(list = list),
            )
            nodes.plus(key to added)
        }
        return added
    }

    fun addAfter(sibling: SubtaskNode, task: Task, list: CaldavFilter?): SubtaskNode {
        val key = subtaskKey(task)
        var added = SubtaskNode(key = key, parentKey = sibling.parentKey, sequence = 0L, task = task)
        _nodes.update { nodes ->
            val parentKey = nodes[sibling.key]?.parentKey ?: sibling.parentKey
            added = SubtaskNode(
                key = key,
                parentKey = parentKey,
                sequence = nodes.nextSequence(),
                task = task,
                pending = PendingTask(list = list),
            )
            nodes.plus(key to added).slottedAfter(parentKey, sibling.key)
        }
        return _nodes.value[key] ?: added
    }

    fun update(key: String, change: (SubtaskNode) -> SubtaskNode): SubtaskNode? {
        var updated: SubtaskNode? = null
        _nodes.update { nodes ->
            val node = nodes[key] ?: run {
                updated = null
                return@update nodes
            }
            val changed = change(node)
            updated = changed
            if (changed == node) nodes else nodes.plus(key to changed)
        }
        return updated
    }

    fun takePending(key: String): PendingTask? {
        var taken: PendingTask? = null
        _nodes.update { nodes ->
            val node = nodes[key]
            taken = node?.pending
            if (node == null || node.pending == null) nodes else nodes.plus(key to node.copy(pending = null))
        }
        return taken
    }

    fun settle(
        created: Map<String, Task>,
        applied: Map<String, SubtaskNode> = emptyMap(),
    ): Map<String, SubtaskNode> {
        if (created.isEmpty()) {
            return emptyMap()
        }
        val carried = mutableMapOf<String, SubtaskNode>()
        _nodes.update { nodes ->
            carried.clear()
            val settled = mutableMapOf<String, SubtaskNode>()
            created.forEach { (key, task) ->
                val node = nodes[key] ?: return@forEach
                val wrote = applied[key]
                val tick = node.stagedCompleted?.takeUnless { it == wrote?.stagedCompleted }
                val landed = node.copy(
                    task = task.copy(),
                    stagedTitle = node.stagedTitle
                        ?.takeUnless { it == task.title || it == wrote?.stagedTitle },
                    stagedCompleted = tick,
                    moved = node.moved && wrote == null,
                )
                settled[key] = landed
                carried[key] = landed.copy(pending = null)
            }
            if (settled.isEmpty()) nodes else nodes.plus(settled)
        }
        return carried
    }

    data class Staging(
        val title: String?,
        val completed: Boolean?,
    )

    private val SubtaskNode.staging: Staging
        get() = Staging(stagedTitle, stagedCompleted)

    fun setTitle(key: String, title: String): Map<String, Staging> {
        var displaced = emptyMap<String, Staging>()
        _nodes.update { nodes ->
            displaced = emptyMap()
            val node = nodes[key] ?: return@update nodes
            val staged = title.takeIf { it != node.task.title.orEmpty() }
            if (staged == node.stagedTitle) {
                nodes
            } else {
                displaced = mapOf(key to node.staging)
                nodes.plus(key to node.copy(stagedTitle = staged))
            }
        }
        return displaced
    }

    fun revertTitle(key: String): Map<String, Staging> {
        var displaced = emptyMap<String, Staging>()
        _nodes.update { nodes ->
            displaced = emptyMap()
            val node = nodes[key] ?: return@update nodes
            if (node.stagedTitle == null) {
                nodes
            } else {
                displaced = mapOf(key to node.staging)
                nodes.plus(key to node.copy(stagedTitle = null))
            }
        }
        return displaced
    }

    fun setCompleted(key: String, completed: Boolean): Map<String, Staging> {
        val displaced = mutableMapOf<String, Staging>()
        _nodes.update { nodes ->
            displaced.clear()
            if (completed) {
                val node = nodes[key] ?: return@update nodes
                val underneath = nodes.carriedTo(key) ?: node.task.isCompleted
                val staged = true.takeIf { it != underneath }
                if (staged == node.stagedCompleted) {
                    return@update nodes
                }
                displaced[key] = node.staging
                nodes.plus(key to node.copy(stagedCompleted = staged))
            } else {
                nodes.unTicking(key, displaced)
            }
        }
        return displaced.toMap()
    }

    private fun Map<String, SubtaskNode>.unTicking(
        key: String,
        displaced: MutableMap<String, Staging>,
    ): Map<String, SubtaskNode> {
        var working = this
        var current = key
        var staledFiled = false
        val seen = mutableSetOf(current)
        while (true) {
            val node = working[current] ?: break
            when {
                node.stagedCompleted == true -> {
                    displaced[current] = node.staging
                    working = working.plus(current to node.copy(stagedCompleted = null))
                }
                !staledFiled && node.stagedCompleted == null && node.task.isCompleted -> {
                    displaced[current] = node.staging
                    working = working.plus(current to node.copy(stagedCompleted = false))
                    staledFiled = true
                }
            }
            current = node.parentKey
            if (!seen.add(current)) {
                break
            }
        }
        return if (displaced.isEmpty()) this else working
    }

    fun setCollapsed(key: String, collapsed: Boolean) {
        _nodes.update { nodes ->
            val node = nodes[key] ?: return@update nodes
            nodes.plus(key to node.copy(task = node.task.copy(isCollapsed = collapsed)))
        }
    }

    fun setList(rootKey: String, list: CaldavFilter?) {
        _nodes.update { nodes ->
            val updated = nodes
                .subtreeOf(rootKey)
                .filterValues { it.pending != null && it.pending.list != list }
                .mapValues { (_, node) -> node.copy(pending = node.pending?.copy(list = list)) }
            if (updated.isEmpty()) nodes else nodes.plus(updated)
        }
    }

    fun delete(key: String) {
        setDeleted(key, deleted = true)
    }

    fun restore(key: String) {
        setDeleted(key, deleted = false)
    }

    private fun setDeleted(key: String, deleted: Boolean) {
        _nodes.update { nodes ->
            val node = nodes[key] ?: return@update nodes
            if (node.deleted == deleted) nodes else nodes.plus(key to node.copy(deleted = deleted))
        }
    }

    fun drop(key: String) {
        _nodes.update { nodes ->
            if (nodes[key] == null) nodes else LinkedHashMap(nodes).apply { remove(listOf(key)) }
        }
    }

    fun revert(staging: Map<String, Staging>) {
        if (staging.isEmpty()) {
            return
        }
        _nodes.update { nodes ->
            val reverted = staging.mapNotNull { (key, was) ->
                val node = nodes[key] ?: return@mapNotNull null
                if (node.stagedTitle == was.title && node.stagedCompleted == was.completed) {
                    return@mapNotNull null
                }
                key to node.copy(stagedTitle = was.title, stagedCompleted = was.completed)
            }
            if (reverted.isEmpty()) nodes else nodes.plus(reverted)
        }
    }

    fun restoreDeletions(deletions: Map<String, Boolean>) {
        if (deletions.isEmpty()) {
            return
        }
        _nodes.update { nodes ->
            val restored = deletions.mapNotNull { (key, deleted) ->
                val node = nodes[key] ?: return@mapNotNull null
                if (node.deleted == deleted) null else key to node.copy(deleted = deleted)
            }
            if (restored.isEmpty()) nodes else nodes.plus(restored)
        }
    }

    data class Arrangement(val parentKey: String, val sequence: Long, val moved: Boolean)

    fun arrangementUnder(rootKey: String): Map<String, Arrangement> =
        _nodes.value.subtreeOf(rootKey).mapValues { (_, node) ->
            Arrangement(node.parentKey, node.sequence, node.moved)
        }

    fun restoreArrangement(rootKey: String, arrangements: Map<String, Arrangement>) {
        if (arrangements.isEmpty()) {
            return
        }
        _nodes.update { nodes ->
            var working = nodes
            var changed = false
            var progress = true
            while (progress) {
                progress = false
                arrangements.forEach { (key, at) ->
                    val node = working[key] ?: return@forEach
                    if (node.parentKey == at.parentKey &&
                        node.sequence == at.sequence &&
                        node.moved == at.moved
                    ) {
                        return@forEach
                    }
                    if (at.parentKey != rootKey && at.parentKey !in working) {
                        return@forEach
                    }
                    if (at.parentKey == key || working.descendsFrom(at.parentKey, key)) {
                        return@forEach
                    }
                    working = working.plus(
                        key to node.copy(
                            parentKey = at.parentKey,
                            sequence = at.sequence,
                            moved = at.moved,
                        )
                    )
                    progress = true
                    changed = true
                }
            }
            if (changed) working else nodes
        }
    }

    fun move(key: String, parentKey: String, after: String?) {
        _nodes.update { it.moving(key, parentKey, after) }
    }

    private fun Map<String, SubtaskNode>.moving(
        key: String,
        parentKey: String,
        after: String?,
    ): Map<String, SubtaskNode> {
        val node = this[key] ?: return this
        if (key == parentKey || descendsFrom(parentKey, key)) {
            return this
        }
        return plus(key to node.copy(parentKey = parentKey, sequence = nextSequence(), moved = true))
            .slottedAfter(parentKey, after)
    }

    private fun Map<String, SubtaskNode>.slottedAfter(
        parentKey: String,
        after: String?,
    ): Map<String, SubtaskNode> {
        val siblings = childrenOf(parentKey)
        val target = when (after) {
            null -> 0
            else -> siblings.indexOfFirst { it.key == after }.takeIf { it >= 0 }?.plus(1)
        } ?: return this
        return reordered(siblings, from = siblings.lastIndex, to = target)
    }

    fun indent(key: String): Boolean = reparent(key) { nodes, node ->
        val siblings = nodes.childrenOf(node.parentKey)
        siblings.getOrNull(siblings.indexOfFirst { it.key == key } - 1)?.let { previous ->
            Reparented(parentKey = previous.key, after = null, toEnd = true)
        }
    }

    fun outdent(rootKey: String, key: String): Boolean = reparent(key) { nodes, node ->
        node.parentKey
            .takeIf { it != rootKey }
            ?.let { nodes[it] }
            ?.let { parent ->
                Reparented(parentKey = parent.parentKey, after = parent.key, toEnd = false)
            }
    }

    private data class Reparented(val parentKey: String, val after: String?, val toEnd: Boolean)

    private fun reparent(
        key: String,
        landing: (Map<String, SubtaskNode>, SubtaskNode) -> Reparented?,
    ): Boolean {
        var moved = false
        _nodes.update { nodes ->
            moved = false
            val node = nodes[key] ?: return@update nodes
            val to = landing(nodes, node) ?: return@update nodes
            moved = true
            val updated = nodes.plus(
                key to node.copy(
                    parentKey = to.parentKey,
                    sequence = nodes.nextSequence(),
                    moved = true,
                )
            )
            if (to.toEnd) updated else updated.slottedAfter(to.parentKey, to.after)
        }
        return moved
    }

    fun clearWritten(written: Collection<SubtaskNode>) {
        if (written.isEmpty()) {
            return
        }
        _nodes.update { nodes ->
            val done = written
                .filter { nodes[it.key]?.sameStagingAs(it) == true }
                .mapTo(mutableSetOf()) { it.key }
            val held = mutableSetOf<String>()
            nodes.keys.forEach { key ->
                if (key !in done) {
                    nodes.ancestorsOf(key).forEach { held.add(it) }
                }
            }
            val dropped = written.map { it.key }.filter { it in done && it !in held }
            if (dropped.isEmpty()) nodes else LinkedHashMap(nodes).apply { remove(dropped) }
        }
    }
}
