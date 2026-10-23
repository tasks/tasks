package org.tasks.extensions

/**
 * Whether this build runs on a desktop platform (Windows, macOS or Linux).
 *
 * Used to select the pull-to-refresh trigger for the task list: desktop uses
 * [org.tasks.compose.tasklist.WheelPullToRefreshFallback] and disables the
 * [androidx.compose.material3.pulltorefresh.PullToRefreshBox]'s own pull path,
 * which misbehaves with wheel and trackpad input (see the fallback's KDoc for
 * the library evidence); Android and iOS keep the box's native touch path.
 */
expect val isDesktop: Boolean
