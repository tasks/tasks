package org.tasks.process

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.di.Platform
import org.tasks.di.platform
import java.io.File

class ProcessRunnerTest {
    @Test
    fun capturesOutputAndExitCode() {
        val result = shell("echo hello")!!

        assertEquals(0, result.exitCode)
        assertEquals("hello", result.output.trim())
    }

    @Test
    fun reportsANonZeroExit() {
        val result = shell("exit 3")!!

        assertEquals(3, result.exitCode)
    }

    @Test
    fun answersNullWhenTheHelperIsNotThere() {
        val result = ProcessBuilder("tasks-definitely-not-a-real-helper")
            .runToCompletion(TIMEOUT_SECONDS, "test", "missing helper")

        assertNull(result)
    }

    @Test(timeout = 60_000)
    fun readsMoreOutputThanThePipeBufferHolds() {
        val result = shell(emitLines(LARGE_OUTPUT_LINES))!!

        assertEquals(0, result.exitCode)
        assertEquals(LARGE_OUTPUT_LINES, result.output.trim().lines().size)
    }

    @Test(timeout = 60_000)
    fun killsAHelperThatOverrunsTheTimeout() {
        val marker = File("build/tmp/process-runner-overrun.marker")
        marker.parentFile.mkdirs()
        marker.delete()

        val result = shell(sleepThenWrite(OVERRUN_SECONDS, marker), timeoutSeconds = 1)

        assertNull(result)

        Thread.sleep((OVERRUN_SECONDS + 3) * 1_000L)
        assertFalse(
            "the helper outlived the call that started it and finished its work anyway",
            marker.exists(),
        )
    }

    private fun shell(script: String, timeoutSeconds: Long = TIMEOUT_SECONDS): ProcessOutput? =
        ProcessBuilder(
            *when (platform()) {
                Platform.WINDOWS -> arrayOf("cmd.exe", "/c", script)
                else -> arrayOf("/bin/sh", "-c", script)
            }
        )
            .redirectErrorStream(true)
            .runToCompletion(timeoutSeconds, "test", "shell")

    private fun emitLines(count: Int): String = when (platform()) {
        Platform.WINDOWS -> "for /L %i in (1,1,$count) do @echo $LINE"
        else -> "i=0; while [ \$i -lt $count ]; do echo $LINE; i=\$((i+1)); done"
    }

    private fun sleepThenWrite(seconds: Int, marker: File): String = when (platform()) {
        Platform.WINDOWS ->
            "ping -n ${seconds + 1} 127.0.0.1 > nul & echo done > ${marker.path.replace('/', '\\')}"
        else -> "sleep $seconds; echo done > ${marker.path}"
    }

    companion object {
        private const val TIMEOUT_SECONDS = 10L

        private const val OVERRUN_SECONDS = 4

        private const val LARGE_OUTPUT_LINES = 2_000

        private val LINE = "0123456789".repeat(10)
    }
}
