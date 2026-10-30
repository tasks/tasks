package org.tasks.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.tasks.analytics.ExceptionThrottle.Decision.Drop
import org.tasks.analytics.ExceptionThrottle.Decision.Report

class ExceptionThrottleTest {
    private var time = 0L
    private val throttle = ExceptionThrottle(
        perFingerprintLimit = 2,
        globalLimit = 3,
        windowMillis = 1000,
        now = { time },
    )

    private val booms = List(10) { IllegalStateException("id $it") }.iterator()
    private val others = List(10) { IllegalArgumentException() }.iterator()

    private fun boom() = booms.next()
    private fun other() = others.next()

    @Test
    fun sameExceptionLimitedPerWindow() {
        val decisions = (1..5).map { throttle.check(boom()) }
        assertEquals(listOf(Report(0), Report(0), Drop, Drop, Drop), decisions)
    }

    @Test
    fun suppressedCountCarriedToNextWindow() {
        repeat(5) { throttle.check(boom()) }
        time = 1000
        assertEquals(Report(3), throttle.check(boom()))
        assertEquals(Report(0), throttle.check(boom()))
    }

    @Test
    fun globalLimitAppliesAcrossFingerprints() {
        throttle.check(boom())
        throttle.check(boom())
        throttle.check(other())
        assertEquals(Drop, throttle.check(other()))
        time = 1000
        assertEquals(Report(1), throttle.check(other()))
    }

    @Test
    fun fingerprintIgnoresMessage() {
        assertEquals(
            ExceptionThrottle.fingerprint(boom()),
            ExceptionThrottle.fingerprint(boom()),
        )
    }

    @Test
    fun sameLibraryFramesFromDifferentAppCodeAreDistinct() {
        assertNotEquals(
            ExceptionThrottle.fingerprint(ioException("org.tasks.logging.FileLogWriter")),
            ExceptionThrottle.fingerprint(ioException("org.tasks.backup.TasksJsonExporter")),
        )
    }

    @Test
    fun sameLibraryFramesFromSameAppCodeMatch() {
        assertEquals(
            ExceptionThrottle.fingerprint(ioException("org.tasks.logging.FileLogWriter")),
            ExceptionThrottle.fingerprint(ioException("org.tasks.logging.FileLogWriter")),
        )
    }

    private fun ioException(caller: String) = java.io.IOException("No space left on device").apply {
        stackTrace = arrayOf(
            StackTraceElement("sun.nio.ch.UnixFileDispatcherImpl", "write0", null, -2),
            StackTraceElement("sun.nio.ch.UnixFileDispatcherImpl", "write", "UnixFileDispatcherImpl.java", 65),
            StackTraceElement("sun.nio.ch.IOUtil", "writeFromNativeBuffer", "IOUtil.java", 137),
            StackTraceElement("sun.nio.ch.IOUtil", "write", "IOUtil.java", 102),
            StackTraceElement("sun.nio.ch.IOUtil", "write", "IOUtil.java", 72),
            StackTraceElement("okio.OutputStreamSink", "write", "JvmOkio.kt", 57),
            StackTraceElement(caller, "write", caller.substringAfterLast('.') + ".kt", 59),
        )
    }
}
