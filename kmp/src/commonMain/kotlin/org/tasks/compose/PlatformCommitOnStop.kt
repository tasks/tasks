package org.tasks.compose

import androidx.compose.runtime.Composable

/**
 * Returns a probe for "the UI is going away for real, so commit what the open editor is holding".
 *
 * Android has no other chance: the process can be killed while stopped, so an edit that isn't
 * written by then is gone. It still has to tell a real departure from a rotation, which destroys
 * and recreates the activity and from a lifecycle callback looks identical.
 *
 * Desktop never commits from here. Stopping there means the window was minimized - the process
 * keeps running and the editor comes back exactly as it was - and a desktop user expects unsaved
 * work to stay unsaved until they ask for it or close the app.
 */
@Composable
expect fun rememberShouldCommitEditsOnStop(): () -> Boolean

/**
 * Blocks the calling thread for up to [timeoutMs] while [awaitIdle] runs, so that a commit asked for
 * on the way out has a chance to land before the caller returns.
 *
 * Android only, and the counterpart to [rememberShouldCommitEditsOnStop]. The commit is posted to an
 * app-scoped dispatcher, and once ON_STOP returns the process is a background-kill candidate - so
 * without a wait here the save this whole path exists for could be killed before its worker thread
 * ever ran. Bounded, because ON_STOP is on the main thread and a slow sync adapter must not stall
 * the stop; the desktop actual does nothing, since desktop never commits from ON_STOP at all.
 */
expect fun blockForPendingCommits(timeoutMs: Long, awaitIdle: suspend () -> Unit)
