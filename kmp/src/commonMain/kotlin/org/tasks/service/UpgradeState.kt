package org.tasks.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

class UpgradeState {
    private val done = CompletableDeferred<Unit>()

    @Volatile
    var isUpgrading = false
        private set

    val isReady: Boolean
        get() = done.isCompleted

    suspend fun awaitReady(caller: String) {
        if (isReady) {
            return
        }
        Logger.i(TAG) { "$caller waiting for upgrade" }
        val start = TimeSource.Monotonic.markNow()
        done.await()
        Logger.i(TAG) { "$caller resumed after ${start.elapsedNow().inWholeMilliseconds}ms" }
    }

    fun ready() {
        if (done.complete(Unit)) {
            Logger.i(TAG) { "Ready" }
        }
    }

    fun launch(
        scope: CoroutineScope,
        upgrading: Boolean = false,
        block: suspend () -> Unit,
    ): Job {
        isUpgrading = upgrading
        return scope.launch(Dispatchers.IO) {
            val start = TimeSource.Monotonic.markNow()
            block()
            Logger.i(TAG) { "Startup finished in ${start.elapsedNow().inWholeMilliseconds}ms" }
            isUpgrading = false
            ready()
        }
    }

    companion object {
        private const val TAG = "UpgradeState"
    }
}
