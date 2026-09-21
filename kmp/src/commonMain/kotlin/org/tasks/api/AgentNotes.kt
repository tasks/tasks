package org.tasks.api

object AgentNotes {
    const val LOCAL_IDS =
        "Ids are local to this install. Never store them; resolve names to ids each session."

    const val RESOLVE_NAMES =
        "Rows reference each other by id, never by name. Resolve a name the user said to an id " +
            "before acting on it, and do not read an id back to them."

    const val CARDINALITY =
        "A task belongs to exactly one list and sits at no more than one place, but carries any " +
            "number of tags."

    const val REMINDERS =
        "Reminders are one collection covering both kinds: a time reminder carries a trigger " +
            "time or an offset, a location reminder carries a place and no timing. Arrival and " +
            "departure are separate, independent reminders."

    const val PAGING =
        "Every read is paged: it takes a limit and an offset, reports a total that ignores both, " +
            "and flags whether more remain. There is no way to ask for all of them, so do not " +
            "assume one call saw everything - a user can have hundreds of tags."

    const val COUNTING =
        "To count, ask for a limit of zero: the total comes back with no rows. Never list rows " +
            "in order to count them."

    const val MATCHES =
        "Text search is a regular expression over a task's own title and notes. Narrow it to one " +
            "of the two when you mean one, and escape a literal dot or slash."

    const val READ_ONLY_LISTS =
        "Some lists are read-only. A task on one says so, and writing to it fails rather than " +
            "quietly doing nothing."

    const val BATCH_WRITES =
        "Creating, updating and completing tasks are batch operations: each takes a list of " +
            "tasks and runs as one transaction. Send the whole set rather than calling once per " +
            "task."

    const val NEW_TASK_DEFAULTS =
        "A new task gets nothing you do not send - no priority, dates, recurrence, reminders or " +
            "tags - and lands in the user's default list unless you name one."

    fun timeZone(zoneId: String) =
        "The device's time zone is $zoneId. Every iso timestamp is in it, with no offset, so " +
            "work out today, tomorrow and this week in it."

    const val DELETE_CASCADE =
        "Deleting a task deletes its subtasks with it, and no delete can be undone."

    val READS = listOf(LOCAL_IDS, RESOLVE_NAMES, CARDINALITY, REMINDERS, PAGING, COUNTING, MATCHES)

    val WRITES = listOf(READ_ONLY_LISTS, BATCH_WRITES)

    val ALL = READS + WRITES + NEW_TASK_DEFAULTS + DELETE_CASCADE
}
