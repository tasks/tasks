package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.caldav.Task.Companion.tasksFromReader
import java.io.StringReader

/**
 * Tests that RFC 5545 line folding is correctly handled when iCalendar data
 * arrives with bare LF line endings (as happens when XML parsers normalize
 * CRLF to LF in DAV REPORT responses).
 *
 * These tests run on the JVM classpath (without ical4android) to verify that
 * kmp/src/jvmCommonMain/resources/ical4j.properties provides the necessary
 * ical4j.unfolding.relaxed=true setting.
 *
 * See: https://datatracker.ietf.org/doc/html/rfc5545#section-3.1
 */
class LineFoldingTest {

    @Test
    fun foldedSummaryIsUnfolded() {
        val task = parse("folding/long_summary.txt")
        assertEquals(
            "https://android-developers.googleblog.com/2025/11/raising-bar-on-battery-performance.html",
            task.summary,
        )
    }

    @Test
    fun foldedSummaryDoesNotLeakNextProperty() {
        val task = parse("folding/long_summary.txt")
        assertEquals(false, task.summary?.contains("X-APPLE-SORT-ORDER"))
    }

    @Test
    fun foldedDescriptionIsUnfolded() {
        val task = parse("folding/long_description.txt")
        assertEquals(
            "[Google](www.google.com) link with *bold*\n\nLet's make this longerrrr, shall we? Here is _underline_ and **bold**\n\nAnd another line.",
            task.description,
        )
    }

    @Test
    fun multipleFoldedLinesInSummary() {
        val task = parse("folding/multiple_folded_lines.txt")
        assertEquals(
            "This is a very long task title that will definitely need to be folded across multiple lines because it exceeds the seventy-five byte line limit that RFC 5545 specifies for iCalendar content lines",
            task.summary,
        )
    }

    @Test
    fun multipleFoldedLinesInDescription() {
        val task = parse("folding/multiple_folded_lines.txt")
        assertEquals(
            "First paragraph with enough text to trigger folding in the description property as well.\n\nSecond paragraph with **markdown** and _italic_ formatting.\n\nThird paragraph.",
            task.description,
        )
    }

    private fun parse(path: String): Task {
        val content = javaClass.classLoader!!.getResource(path)!!.readText()
        val tasks = tasksFromReader(StringReader(content))
        assertEquals(1, tasks.size)
        return tasks[0]
    }
}
