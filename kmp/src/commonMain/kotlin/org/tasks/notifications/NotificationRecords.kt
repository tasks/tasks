package org.tasks.notifications

fun undeliveredRows(
    attempted: Collection<Long>,
    delivered: Collection<Long>,
    existing: Set<Long>,
): List<Long> {
    val posted = delivered.toSet()
    return attempted.filter { it !in posted && it !in existing }
}
