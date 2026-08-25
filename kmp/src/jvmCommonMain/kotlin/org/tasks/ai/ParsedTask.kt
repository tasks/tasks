package org.tasks.ai

import kotlinx.serialization.Serializable

@Serializable
data class ParsedTaskList(val tasks: List<ParsedTask> = emptyList())

@Serializable
data class ParsedTask(
    val title: String,
    val notes: String? = null,
    val list: String? = null,
    val tags: List<String> = emptyList(),
    /** Local time, "2026-09-01" or "2026-09-01T14:00". */
    val due: String? = null,
    val start: String? = null,
    /** high | medium | low | none */
    val priority: String? = null,
    /** Bare RFC-5545 RRULE, e.g. "FREQ=WEEKLY;BYDAY=MO". */
    val recurrence: String? = null,
)
