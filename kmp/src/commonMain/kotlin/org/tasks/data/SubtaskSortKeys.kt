package org.tasks.data

import org.tasks.data.entity.Task

internal fun sortKeysFor(rows: List<Task?>, parentId: Long): List<Long> {
    val stored = rows.map { row -> row?.order?.takeIf { row.parent == parentId } }
    val keys = arrayOfNulls<Long>(rows.size)
    stored.longestRun().forEach { keys[it] = stored[it] }
    var index = 0
    while (index < rows.size) {
        if (keys[index] != null) {
            index++
            continue
        }
        val start = index
        while (index < rows.size && keys[index] == null) {
            index++
        }
        val count = index - start
        val low = keys.getOrNull(start - 1)
        val high = keys.getOrNull(index)
        val step = when {
            low != null && high != null -> (high - low) / (count + 1)
            low != null -> if (low > Long.MAX_VALUE - count) 0L else 1L
            high != null -> if (high < Long.MIN_VALUE + count) 0L else 1L
            else -> 1L
        }
        if (step < 1L) {
            return rows.indices.map { it.toLong() }
        }
        val first = when {
            low != null -> low + step
            high != null -> high - count
            else -> 0L
        }
        repeat(count) { keys[start + it] = first + step * it }
    }
    return keys.map { it!! }
}

internal fun List<Long?>.longestRun(): List<Int> {
    val length = IntArray(size)
    val previous = IntArray(size) { -1 }
    var end = -1
    forEachIndexed { index, value ->
        if (value == null) {
            return@forEachIndexed
        }
        length[index] = 1
        for (before in 0 until index) {
            val earlier = this[before] ?: continue
            if (earlier < value && length[before] + 1 > length[index]) {
                length[index] = length[before] + 1
                previous[index] = before
            }
        }
        if (end < 0 || length[index] > length[end]) {
            end = index
        }
    }
    val kept = mutableListOf<Int>()
    var at = end
    while (at >= 0) {
        kept.add(at)
        at = previous[at]
    }
    return kept
}
