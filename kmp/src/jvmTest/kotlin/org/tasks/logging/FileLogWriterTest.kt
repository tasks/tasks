package org.tasks.logging

import co.touchlab.kermit.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch

class FileLogWriterTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun writesStraightToDiskOnceShuttingDown() {
        val writer = FileLogWriter(folder.root)

        writer.beginShutdown()
        writer.log(Severity.Info, "closing up", "Startup", null)

        assertTrue(currentLog().readText().contains("Startup                 I closing up"))
    }

    @Test
    fun keepsTheStackTraceOfAThrowableLoggedWhileShuttingDown() {
        val writer = FileLogWriter(folder.root)

        writer.beginShutdown()
        writer.log(Severity.Warn, "it broke", "Startup", IllegalStateException("boom"))

        val written = currentLog().readText()
        assertTrue(written.contains("W it broke"))
        assertTrue(written.contains("IllegalStateException: boom"))
    }

    @Test
    fun stampsShutdownLinesTheSameWayAsTheRest() {
        val writer = FileLogWriter(folder.root)

        writer.beginShutdown()
        writer.log(Severity.Info, "closing up", "Startup", null)

        val line = currentLog().readLines().first { it.contains("closing up") }
        assertTrue(line, TIMESTAMPED.matches(line))
    }

    @Test
    fun truncatesLongTagsOnShutdownLinesToo() {
        val writer = FileLogWriter(folder.root)

        writer.beginShutdown()
        writer.log(Severity.Debug, "still here", "NucleusWindowsNotifications", null)

        val line = currentLog().readLines().first { it.contains("still here") }
        assertEquals("NucleusWin...ifications D still here", line.substringAfter("Z "))
    }

    @Test
    fun keepsWhatWasAlreadyWrittenWhenShutdownTakesTheFileOver() {
        val writer = FileLogWriter(folder.root)
        writer.log(Severity.Info, "while running", "Startup", null)
        eventually { currentLog().readText().contains("while running") }

        writer.beginShutdown()
        writer.log(Severity.Info, "closing up", "Startup", null)

        val written = currentLog().readText()
        assertTrue(written, written.indexOf("while running") < written.indexOf("closing up"))
    }

    @Test
    fun drainsQueuedWritesBeforeTakingTheFileOver() {
        val writer = FileLogWriter(folder.root)
        repeat(50) { writer.log(Severity.Info, "queued $it", "Startup", null) }

        writer.beginShutdown()
        writer.log(Severity.Info, "closing up", "Startup", null)

        val written = currentLog().readText()
        repeat(50) { assertTrue("missing queued $it", written.contains("queued $it")) }
        assertTrue(written.indexOf("queued 49") < written.indexOf("closing up"))
    }

    @Test
    fun aSecondWriterDoesNotPublishIntoTheFirstsFile() {
        val first = FileLogWriter(folder.newFolder("first"))
        val second = FileLogWriter(folder.newFolder("second"))

        second.log(Severity.Info, "only the second", "Startup", null)
        second.beginShutdown()
        first.beginShutdown()

        assertTrue(File(folder.root, "second/log.0.txt").readText().contains("only the second"))
        assertEquals("", File(folder.root, "first/log.0.txt").readText())
    }

    @Test
    fun shuttingDownTwiceTakesTheFileOverOnce() {
        val writer = FileLogWriter(folder.root)

        writer.beginShutdown()
        writer.beginShutdown()
        writer.log(Severity.Info, "closing up", "Startup", null)

        assertEquals(1, currentLog().readLines().count { it.contains("closing up") })
    }

    @Test
    fun nothingLoggedWhileTheFileIsHandedOverIsLost() {
        val writer = FileLogWriter(folder.root)
        val start = CountDownLatch(1)
        val racing = (0 until 20).map { i ->
            Thread {
                start.await()
                writer.log(Severity.Info, "racing $i", "Startup", null)
            }.apply { start() }
        }

        start.countDown()
        writer.beginShutdown()
        racing.forEach { it.join() }

        val written = currentLog().readText()
        repeat(20) { assertTrue("missing racing $it", written.contains("racing $it")) }
    }

    private fun eventually(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Timed out waiting for the log to be written")
    }

    private fun currentLog() = File(folder.root, "log.0.txt")

    companion object {
        private val TIMESTAMPED =
            Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z .*""")
    }
}
