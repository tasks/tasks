package org.tasks.compose

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@Composable
actual fun rememberShouldCommitEditsOnStop(): () -> Boolean {
    val activity = LocalActivity.current
    // Read through the activity on every call rather than capturing the flag: it is only set for
    // the duration of the teardown that a lifecycle callback is reacting to.
    return remember(activity) { { activity?.isChangingConfigurations != true } }
}

actual fun blockForPendingCommits(timeoutMs: Long, awaitIdle: suspend () -> Unit) {
    runBlocking {
        if (withTimeoutOrNull(timeoutMs) { awaitIdle() } == null) {
            Logger.w(tag = "CommitOnStop") { "Timed out committing edits on stop" }
        }
    }
}
