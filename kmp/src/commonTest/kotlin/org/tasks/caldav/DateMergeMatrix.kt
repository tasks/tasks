package org.tasks.caldav

import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.icalendar.VTodo
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import org.tasks.time.DateTimeUtils2.setCurrentMillisFixed
import org.tasks.time.DateTimeUtils2.setCurrentMillisSystem
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

object DateMergeMatrix {
    const val NOW = 1794744000123L
    private const val EDITED = NOW - 5 * 3_600_000L - 777

    private val INSTANTS = listOf(
        "20200101T000000",
        "20260308T073000",
        "20261101T063000",
        "20261115T115959",
        "20300101T000000",
    )

    private val VALUES: List<String?> = listOf(null) + INSTANTS.flatMap { listOf("${it}Z", it) }

    private val MILLIS = listOf(
        1577836800000L,
        1577836800999L,
        1772955000123L,
        1793514600500L,
        1794743999001L,
        NOW,
    )

    private class Combination(val dtStamp: String?, val created: String?, val lastModified: String?) {
        val ics = buildString {
            append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Other//Client//EN\r\nBEGIN:VTODO\r\nUID:u\r\n")
            dtStamp?.let { append("DTSTAMP:$it\r\n") }
            created?.let { append("CREATED:$it\r\n") }
            lastModified?.let { append("LAST-MODIFIED:$it\r\n") }
            append("SUMMARY:s\r\nEND:VTODO\r\nEND:VCALENDAR\r\n")
        }

        override fun toString() = "DTSTAMP=$dtStamp CREATED=$created LAST-MODIFIED=$lastModified"
    }

    private val combinations = VALUES.flatMap { dtStamp ->
        VALUES.flatMap { created -> VALUES.map { lastModified -> Combination(dtStamp, created, lastModified) } }
    }

    private fun Combination.parse(): VTodo? =
        try {
            parseVTodos(ics).singleOrNull()
        } catch (_: Exception) {
            null
        }

    private fun Task.dates() = creationDate to modificationDate

    private fun Long.wholeSecond() = this - this % 1000

    private fun firstDownload(remote: VTodo) = Task(creationDate = NOW).applyRemote(remote, null)

    private fun tasksCopy(task: Task) =
        VTodo(uid = "u").apply { applyLocal(CaldavTask(calendar = "c"), task) }.serialize()

    fun <T> frozen(block: () -> T): T {
        setCurrentMillisFixed(NOW)
        try {
            return block()
        } finally {
            setCurrentMillisSystem()
        }
    }

    private fun check(failures: List<String>, cases: Int) {
        assertTrue(cases > 0)
        if (failures.isNotEmpty()) {
            fail("${failures.size} of $cases cases failed:\n" + failures.take(25).joinToString("\n"))
        }
    }

    fun unchangedCopiesKeepBothDates() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (combination in combinations) {
            val remote = combination.parse() ?: continue
            cases++
            val task = firstDownload(remote)
            val expected = task.dates()
            repeat(3) {
                task.applyRemote(combination.parse()!!, combination.parse()!!)
                if (task.dates() != expected) {
                    failures += "$combination: download ${it + 2} changed $expected to ${task.dates()}"
                }
            }
        }
        check(failures, cases)
        cases
    }

    fun unchangedCompletedCopiesKeepTheirCompletionDate() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (completed in VALUES.filterNotNull()) {
            for (lastModified in VALUES) {
                val ics = buildString {
                    append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Other//Client//EN\r\nBEGIN:VTODO\r\nUID:u\r\n")
                    append("DTSTAMP:20200101T000000Z\r\n")
                    lastModified?.let { append("LAST-MODIFIED:$it\r\n") }
                    append("STATUS:COMPLETED\r\nCOMPLETED:$completed\r\nSUMMARY:s\r\nEND:VTODO\r\nEND:VCALENDAR\r\n")
                }
                val remote = try { parseVTodos(ics).singleOrNull() } catch (_: Exception) { null } ?: continue
                cases++
                val task = firstDownload(remote)
                val expected = Triple(task.creationDate, task.modificationDate, task.completionDate)
                repeat(3) {
                    task.applyRemote(parseVTodos(ics).single(), parseVTodos(ics).single())
                    val actual = Triple(task.creationDate, task.modificationDate, task.completionDate)
                    if (actual != expected) {
                        failures += "COMPLETED=$completed LAST-MODIFIED=$lastModified: download ${it + 2} changed $expected to $actual"
                    }
                }
            }
        }
        check(failures, cases)
        cases
    }

    fun unparseableCopiesFailTheSameWayEveryTime() = frozen {
        for (combination in combinations) {
            assertEquals(combination.parse() == null, combination.parse() == null, combination.toString())
        }
    }

    fun copiesWrittenByTasksOnlyLoseMilliseconds() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (created in MILLIS) {
            for (modified in MILLIS.filter { it >= created }) {
                cases++
                val task = Task(creationDate = created, modificationDate = modified)
                val copy = tasksCopy(task)
                val expected = created.wholeSecond() to modified.wholeSecond()
                repeat(3) {
                    task.applyRemote(parseVTodos(copy).single(), parseVTodos(copy).single())
                    if (task.dates() != expected) {
                        failures += "created=$created modified=$modified: download ${it + 1} gave ${task.dates()}, expected $expected"
                    }
                }
            }
        }
        check(failures, cases)
        cases
    }

    fun pushingAnEditAndDownloadingAgainIsStable() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (combination in combinations) {
            val remote = combination.parse() ?: continue
            cases++
            val task = firstDownload(remote)
            val created = task.creationDate
            task.modificationDate = EDITED
            val pushed = tasksCopy(task)
            task.applyRemote(parseVTodos(pushed).single(), parseVTodos(pushed).single())
            val afterPush = task.dates()
            if (task.creationDate != created.wholeSecond()) {
                failures += "$combination: creation $created became ${task.creationDate}"
            }
            if (task.modificationDate < EDITED.wholeSecond()) {
                failures += "$combination: edit at $EDITED went back to ${task.modificationDate}"
            }
            repeat(2) {
                task.applyRemote(parseVTodos(pushed).single(), parseVTodos(pushed).single())
                if (task.dates() != afterPush) {
                    failures += "$combination: download ${it + 2} after push changed $afterPush to ${task.dates()}"
                }
            }
        }
        check(failures, cases)
        cases
    }

    fun localEditsKeepTheirModificationDate() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (combination in combinations) {
            val remote = combination.parse() ?: continue
            if (remote.lastModified == null) continue
            cases++
            val task = firstDownload(remote)
            task.modificationDate = EDITED
            val created = task.creationDate
            task.applyRemote(combination.parse()!!, combination.parse()!!)
            if (task.dates() != created to EDITED) {
                failures += "$combination: local edit at $EDITED became ${task.dates()}"
            }
        }
        check(failures, cases)
        cases
    }

    fun localEditsLoseTheirModificationDateWhenTheCachedCopyHasNoLastModified() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (combination in combinations) {
            val remote = combination.parse() ?: continue
            if (remote.lastModified != null) continue
            cases++
            val task = firstDownload(remote)
            val downloaded = task.dates()
            task.modificationDate = EDITED
            task.applyRemote(combination.parse()!!, combination.parse()!!)
            val expected = if (remote.createdAt == null && remote.dtStamp == null) {
                downloaded.first to EDITED
            } else {
                downloaded
            }
            if (task.dates() != expected) {
                failures += "$combination: local edit at $EDITED became ${task.dates()}, expected $expected"
            }
        }
        check(failures, cases)
        cases
    }

    fun modificationDateMovesUpToCreatedWhenLastModifiedIsOlder() = frozen {
        val failures = mutableListOf<String>()
        var cases = 0
        for (created in MILLIS) {
            for (modified in MILLIS.filter { it.wholeSecond() < created.wholeSecond() }) {
                cases++
                val task = Task(creationDate = created, modificationDate = modified)
                val copy = tasksCopy(task)
                task.applyRemote(parseVTodos(copy).single(), parseVTodos(copy).single())
                if (task.dates() != created.wholeSecond() to created.wholeSecond()) {
                    failures += "created=$created modified=$modified: gave ${task.dates()}"
                }
            }
        }
        check(failures, cases)
        cases
    }
}
