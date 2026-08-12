package org.tasks.data

internal fun Map<String, SubtaskNode>.merged(
    rootKey: String,
    rootId: Long,
    rows: List<TaskContainer>,
): Map<String, SubtaskNode> {
    val nodes = this
    val keysById = rows.associate { it.id to subtaskKey(it.task) }
    val incoming = rows.associateBy { subtaskKey(it.task) }
    val updated = LinkedHashMap(nodes)
    val answeredFor = nodes.rowDescendantsOf(rootId)
    updated.remove(
        nodes
            .subtreeOf(rootKey)
            .keys
            .filter { key -> key in answeredFor && key !in incoming }
    )
    val arrived = mutableListOf<String>()
    rows.forEach { row ->
        val key = subtaskKey(row.task)
        val parentKey = keysById[row.task.parent].takeIf { row.task.parent != rootId }
            ?: rootKey
        val current = updated[key]
        when {
            current == null -> {
                updated[key] = SubtaskNode(
                    key = key,
                    parentKey = parentKey,
                    sequence = 0L,
                    task = row.task,
                )
                arrived.add(key)
            }
            !current.moved &&
                    current.parentKey != parentKey &&
                    !updated.descendsFrom(parentKey, key) -> {
                updated[key] = current.copy(
                    task = row.task,
                    parentKey = parentKey,
                    sequence = 0L,
                )
                arrived.add(key)
            }
            else -> {
                updated[key] = current.copy(task = row.task)
            }
        }
    }
    if (arrived.isNotEmpty()) {
        updated.placeArrivals(arrived, rows)
    }
    updated.followQueryOrder(rootKey, rows)
    return updated
}

private fun MutableMap<String, SubtaskNode>.followQueryOrder(
    rootKey: String,
    rows: List<TaskContainer>,
) {
    if (rows.isEmpty()) {
        return
    }
    val position = rows.withIndex().associate { (index, row) -> subtaskKey(row.task) to index }
    val index = childIndex()
    subtreeOf(rootKey).keys.plus(rootKey).forEach { parentKey ->
        val run = index[parentKey] ?: return@forEach
        if (run.size < 2 || run.any { it.moved || it.isNew || it.key !in position }) {
            return@forEach
        }
        val wanted = run.sortedBy { position.getValue(it.key) }.map { it.key }
        if (wanted != run.map { it.key }) {
            renumber(parentKey, wanted)
        }
    }
}

private fun MutableMap<String, SubtaskNode>.placeArrivals(
    arrived: List<String>,
    rows: List<TaskContainer>,
) {
    val newKeys = arrived.toSet()
    val queryOrder = rows.map { subtaskKey(it.task) }
    arrived.mapTo(LinkedHashSet()) { getValue(it).parentKey }.forEach { parentKey ->
        val drawn = queryOrder.filter { this[it]?.parentKey == parentKey }
        val order = childrenOf(parentKey).map { it.key }.filterNot { it in newKeys }.toMutableList()
        drawn.forEachIndexed { index, key ->
            if (key !in newKeys) {
                return@forEachIndexed
            }
            val at = drawn
                .take(index)
                .mapNotNull { above -> order.indexOf(above).takeIf { it >= 0 } }
                .maxOrNull()
                ?.plus(1)
                ?: 0
            order.add(at, key)
        }
        renumber(parentKey, order)
    }
}
