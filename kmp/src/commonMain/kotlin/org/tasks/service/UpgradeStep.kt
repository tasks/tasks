package org.tasks.service

import co.touchlab.kermit.Logger
import kotlin.time.TimeSource

class UpgradeStep(
    val version: Int,
    val upgrade: () -> Upgrade,
)

suspend fun List<UpgradeStep>.runFrom(from: Int, setVersion: suspend (Int) -> Unit) {
    if (from <= 0) {
        return
    }
    filter { from < it.version }.sortedBy { it.version }.forEach { step ->
        Logger.i(TAG) { "Running upgrade step ${step.version}" }
        val start = TimeSource.Monotonic.markNow()
        step.upgrade().run()
        setVersion(step.version)
        Logger.i(TAG) { "Finished upgrade step ${step.version} in ${start.elapsedNow().inWholeMilliseconds}ms" }
    }
}

private const val TAG = "Upgrader"
