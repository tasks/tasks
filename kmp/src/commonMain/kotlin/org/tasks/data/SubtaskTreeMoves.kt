package org.tasks.data

import org.tasks.filters.CaldavFilter

fun List<SubtaskRow>.travellingWith(from: Int, dragged: SubtaskRow): IntRange {
    val count = drop(from).takeWhile { it.indent > dragged.indent }.size
    return from until from + count
}

private data class Landing(val others: List<SubtaskRow>, val at: Int)

private fun List<SubtaskRow>.blockAt(from: Int, dragged: SubtaskRow): IntRange =
    from..travellingWith(from + 1, dragged).last

private fun List<SubtaskRow>.without(block: IntRange): List<SubtaskRow> =
    filterIndexed { index, _ -> index !in block }

private fun List<SubtaskRow>.landingFor(from: Int, to: Int): Landing? {
    if (from !in indices || to !in indices || from == to) {
        return null
    }
    val dragged = this[from]
    val block = blockAt(from, dragged)
    if (to in block) {
        return null
    }
    val others = without(block)
    val target = others.indexOfFirst { it.key == this[to].key }
    val at = if (from < to) {
        target + 1 + others.drop(target + 1).takeWhile { it.indent > others[target].indent }.size
    } else {
        target
    }
    return Landing(others = others, at = at)
}

private val SubtaskRow?.deepestUnder: Int
    get() = if (this == null) {
        0
    } else {
        deepestNestingUnder(indent = indent, deleted = node.deleted)
    }

private fun List<SubtaskRow>.depthsAt(landing: Int): IntRange {
    val max = getOrNull(landing - 1).deepestUnder
    val min = getOrNull(landing)?.indent ?: 0
    return minOf(min, max)..max
}

fun List<SubtaskRow>.dropRange(
    from: Int,
    to: Int,
    allowsNesting: Boolean = true,
): IntRange {
    val dragged = getOrNull(from) ?: return 0..0
    if (!allowsNesting) {
        return 0..dragged.indent
    }
    val landing = landingFor(from, to)
        ?: return without(blockAt(from, dragged)).depthsAt(from)
    return landing.others.depthsAt(landing.at)
}

fun List<SubtaskRow>.findParent(indent: Int, landing: Int): SubtaskRow? =
    findParentIndex(indent, landing) { this[it].indent }?.let { this[it] }

data class SubtaskLanding(val parentKey: String, val after: String?, val indent: Int)

fun List<SubtaskRow>.resolveMove(
    from: Int,
    to: Int,
    rootKey: String,
    desiredIndent: Int? = null,
): SubtaskLanding? {
    val dragged = getOrNull(from) ?: return null
    val landing = landingFor(from, to) ?: return null
    val others = landing.others
    val at = landing.at
    val indent = (desiredIndent ?: dragged.indent).coerceIn(others.depthsAt(at))
    val parentKey = if (indent == 0) {
        rootKey
    } else {
        others.findParent(indent, at)?.key ?: rootKey
    }
    val after = (at - 1 downTo 0)
        .firstOrNull { others[it].indent <= indent }
        ?.let { others[it].node }
        ?.takeIf { it.parentKey == parentKey }
        ?.key
    return SubtaskLanding(parentKey = parentKey, after = after, indent = indent)
}
