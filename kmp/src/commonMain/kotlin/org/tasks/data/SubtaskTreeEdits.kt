package org.tasks.data

internal fun Map<String, SubtaskNode>.nextSequence(): Long =
    (values.maxOfOrNull { it.sequence } ?: 0L) + 1L

internal fun Map<String, SubtaskNode>.reordered(
    run: List<SubtaskNode>,
    from: Int,
    to: Int,
): Map<String, SubtaskNode> {
    if (from !in run.indices || to !in run.indices || from == to) {
        return this
    }
    val siblings = run.toMutableList()
    siblings.add(to, siblings.removeAt(from))
    val sequences = siblings.map { it.sequence }.sorted()
    return plus(siblings.mapIndexed { index, node -> node.key to node.copy(sequence = sequences[index]) })
}

internal fun MutableMap<String, SubtaskNode>.renumber(parentKey: String, order: List<String>) {
    val base = nextSequence()
    order.forEachIndexed { index, key ->
        this[key]?.let { this[key] = it.copy(parentKey = parentKey, sequence = base + index) }
    }
}

internal fun MutableMap<String, SubtaskNode>.remove(keys: Collection<String>) {
    keys.forEach { key ->
        val node = this[key] ?: return@forEach
        val orphans = values
            .filter { it.parentKey == key }
            .sortedBy { it.sequence }
            .map { it.key }
        val run = values
            .filter { it.parentKey == node.parentKey }
            .sortedBy { it.sequence }
            .flatMap { if (it.key == key) orphans else listOf(it.key) }
        orphans.forEach { orphan ->
            this[orphan]?.let { this[orphan] = it.copy(parentKey = node.parentKey) }
        }
        remove(key)
        if (orphans.isNotEmpty()) {
            renumber(node.parentKey, run)
        }
    }
}
