package org.tasks.data

internal fun Map<String, SubtaskNode>.rowDescendantsOf(rootId: Long): Set<String> {
    if (rootId <= 0) {
        return emptySet()
    }
    val byRowParent = values.filter { it.id > 0 }.groupBy { it.task.parent }
    val found = mutableSetOf<String>()
    val pending = ArrayDeque(listOf(rootId))
    while (pending.isNotEmpty()) {
        byRowParent[pending.removeFirst()]?.forEach { node ->
            if (found.add(node.key)) {
                pending.add(node.id)
            }
        }
    }
    return found
}

internal fun Map<String, SubtaskNode>.ancestorsOf(key: String): Sequence<String> = sequence {
    var node = this@ancestorsOf[key] ?: return@sequence
    val seen = mutableSetOf(node.key)
    while (seen.add(node.parentKey)) {
        yield(node.parentKey)
        node = this@ancestorsOf[node.parentKey] ?: return@sequence
    }
}

fun Map<String, SubtaskNode>.deletions(): Map<String, Boolean> {
    if (values.none { it.deleted }) {
        return emptyMap()
    }
    return buildMap {
        this@deletions.forEach { (key, node) ->
            if (node.deleted) {
                put(key, true)
                return@forEach
            }
            if (ancestorsOf(key).any { this@deletions[it]?.deleted == true }) {
                put(key, false)
            }
        }
    }
}

internal fun Map<String, SubtaskNode>.carriedTo(key: String): Boolean? =
    ancestorsOf(key).firstNotNullOfOrNull { this[it]?.stagedCompleted }

internal fun Map<String, SubtaskNode>.descendsFrom(key: String, ancestorKey: String): Boolean =
    ancestorsOf(key).any { it == ancestorKey }

internal fun Map<String, SubtaskNode>.childIndex(): Map<String, List<SubtaskNode>> =
    values.groupBy { it.parentKey }.mapValues { (_, run) -> run.sortedBy { it.sequence } }

internal fun Map<String, SubtaskNode>.childrenOf(parentKey: String): List<SubtaskNode> =
    values.filter { it.parentKey == parentKey }.sortedBy { it.sequence }

fun Map<String, SubtaskNode>.subtreeOf(rootKey: String): Map<String, SubtaskNode> {
    val index = childIndex()
    val seen = mutableSetOf(rootKey)
    val found = LinkedHashMap<String, SubtaskNode>()
    val pending = ArrayDeque(index[rootKey].orEmpty())
    while (pending.isNotEmpty()) {
        val node = pending.removeFirst()
        if (!seen.add(node.key)) {
            continue
        }
        found[node.key] = node
        index[node.key]?.let { pending.addAll(it) }
    }
    return found
}

fun Map<String, SubtaskNode>.isRearranged(rootKey: String): Boolean {
    if (values.none { it.needsWriting }) {
        return false
    }
    return subtreeOf(rootKey).values.any { it.needsWriting }
}

fun Map<String, SubtaskNode>.rowsOf(rootKey: String): List<SubtaskRow> =
    childIndex().rowsUnder(this, rootKey, indent = 0, seen = mutableSetOf(rootKey))

internal fun Map<String, List<SubtaskNode>>.rowsUnder(
    nodes: Map<String, SubtaskNode>,
    parentKey: String,
    indent: Int,
    seen: MutableSet<String>,
): List<SubtaskRow> =
    this[parentKey].orEmpty().flatMap { node ->
        if (!seen.add(node.key)) {
            return@flatMap emptyList()
        }
        val nested = rowsUnder(nodes, node.key, indent + 1, seen)
        listOf(
            SubtaskRow(
                node = node,
                indent = indent,
                children = nested.drawable(),
                completed = (node.stagedCompleted ?: nodes.carriedTo(node.key) ?: node.task.isCompleted) &&
                        nested.all { it.completed },
                remaining = nested.remaining(),
            )
        ).plus(nested)
    }

internal inline fun List<SubtaskRow>.eachSwallowed(action: (SubtaskRow, Boolean) -> Unit) {
    var below: Int? = null
    forEach { row ->
        below = below?.takeIf { row.indent > it }
        val swallowed = below != null
        if (!swallowed && row.node.deleted) {
            below = row.indent
        }
        action(row, swallowed)
    }
}

internal fun List<SubtaskRow>.drawable(): Int {
    var count = 0
    eachSwallowed { _, swallowed -> if (!swallowed) count++ }
    return count
}

internal fun List<SubtaskRow>.remaining(): Int {
    var count = 0
    eachSwallowed { row, swallowed ->
        if (!swallowed && !row.node.deleted && !row.completed) count++
    }
    return count
}

fun List<SubtaskRow>.nested(): Boolean {
    var nested = false
    eachSwallowed { row, swallowed ->
        if (!swallowed && !row.node.deleted && row.indent > 0) {
            nested = true
        }
    }
    return nested
}

fun List<SubtaskRow>.visible(): List<SubtaskRow> {
    var hiddenBelow: Int? = null
    return filter { row ->
        val hidden = hiddenBelow
        if (hidden != null && row.indent > hidden) {
            return@filter false
        }
        hiddenBelow = if (row.collapsed || row.node.deleted) row.indent else null
        true
    }
}

fun List<SubtaskRow>.doomed(): Set<String> {
    val doomed = mutableSetOf<String>()
    eachSwallowed { row, swallowed ->
        if (swallowed || row.node.deleted) {
            doomed.add(row.key)
        }
    }
    return doomed
}
