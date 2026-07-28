package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Never. Minimizing dispatches ON_STOP here - Compose Multiplatform maps the minimized window to
 * CREATED - and that is not a departure. Quitting is handled separately, by the flush in main.kt's
 * onCloseRequest, which runs while the composition is still alive, with the shutdown hook there as
 * the backstop for exits that never reach a window at all.
 */
@Composable
actual fun rememberShouldCommitEditsOnStop(): () -> Boolean = remember { { false } }

/** Nothing to wait for: nothing is committed from ON_STOP here. Quitting waits in main.kt instead. */
actual fun blockForPendingCommits(timeoutMs: Long, awaitIdle: suspend () -> Unit) = Unit
