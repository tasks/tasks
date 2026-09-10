package org.tasks.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class ActivityLog(private val capacity: Int = 200) {

    enum class Outcome { Ok, Error, Denied }

    data class Entry(
        val id: Long,
        val at: Instant,
        val tool: String,
        val summary: String,
        val outcome: Outcome,
        val durationMs: Long,
        val detail: String? = null,
    )

    private val ids = AtomicLong()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun record(
        tool: String,
        summary: String,
        outcome: Outcome,
        durationMs: Long,
        detail: String? = null,
    ) {
        val entry = Entry(
            id = ids.incrementAndGet(),
            at = Instant.now(),
            tool = tool,
            summary = summary,
            outcome = outcome,
            durationMs = durationMs,
            detail = detail,
        )

        _entries.update { (listOf(entry) + it).take(capacity) }
    }

    fun clear() = _entries.update { emptyList() }
}
