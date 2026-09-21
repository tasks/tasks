package org.tasks.data

const val INDENT_STEP_DP = 20

fun deepestNestingUnder(
    indent: Int,
    deleted: Boolean = false,
): Int = if (deleted) indent else indent + 1

inline fun findParentIndex(indent: Int, landing: Int, indentAt: (Int) -> Int): Int? {
    if (indent == 0 || landing <= 0) {
        return null
    }
    for (index in landing - 1 downTo 0) {
        if (indent > indentAt(index)) {
            return index
        }
    }
    return null
}
