package org.tasks.process

import co.touchlab.kermit.Logger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class ProcessOutput(val exitCode: Int, val output: String)

internal fun ProcessBuilder.runToCompletion(
    timeoutSeconds: Long,
    tag: String,
    what: String,
): ProcessOutput? {
    val process = try {
        start()
    } catch (e: Exception) {
        Logger.w(throwable = e, tag = tag) { "Failed to start $what" }
        return null
    }
    return try {
        val output = AtomicReference<String>()
        val drain = Thread({
            try {
                process.inputStream.bufferedReader().use { output.set(it.readText()) }
            } catch (e: Exception) {
                Logger.w(throwable = e, tag = tag) { "Failed to read $what" }
            }
        }, "$tag-drain")
        drain.isDaemon = true
        drain.start()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            Logger.w(tag = tag) { "$what timed out" }
            null
        } else {
            drain.join(DRAIN_TIMEOUT_MS)
            when (val read = output.get()) {
                null -> {
                    Logger.w(tag = tag) { "Failed to read all of $what" }
                    null
                }
                else -> ProcessOutput(exitCode = process.exitValue(), output = read)
            }
        }
    } catch (e: Exception) {
        Logger.w(throwable = e, tag = tag) { "$what failed" }
        null
    } finally {
        if (process.isAlive) {
            val descendants = process.descendants().toList()
            process.destroyForcibly()
            descendants.forEach { it.destroyForcibly() }
        }
    }
}

private const val DRAIN_TIMEOUT_MS = 1_000L
