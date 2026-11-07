package org.tasks.auth

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import co.touchlab.kermit.Logger

private val logger = Logger.withTag("OAuthTasks")

@Volatile
private var originTaskId: Int? = null

internal fun Context.rememberOriginTask() {
    originTaskId = appTasks().firstNotNullOfOrNull { task ->
        task.info()?.takeUnless { it.isOAuth }?.stableId
    }
}

internal fun Context.returnToApp() {
    val tasks = appTasks().mapNotNull { task ->
        task.info()?.takeUnless { it.isOAuth }?.let { task to it }
    }
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    val candidates = listOfNotNull(
        tasks.firstOrNull { (_, info) -> info.stableId == originTaskId },
        tasks.firstOrNull { (_, info) ->
            launchIntent?.component != null && info.baseIntent?.component == launchIntent.component
        },
        tasks.firstOrNull(),
    ).distinct()
    if (candidates.none { (task, _) -> task.bringToFront() }) {
        launchIntent?.let { startActivity(it) }
    }
}

internal fun Context.bringBackBrowser(): Boolean =
    appTasks()
        .firstOrNull { task ->
            task.info()?.let { info ->
                val top = info.topActivity?.className
                info.isOAuth && top != null && top != OAuthRedirectActivity::class.java.name
            } == true
        }
        ?.bringToFront() == true

private fun Context.appTasks(): List<ActivityManager.AppTask> =
    applicationContext.getSystemService(ActivityManager::class.java)?.appTasks.orEmpty()

private fun ActivityManager.AppTask.info(): ActivityManager.RecentTaskInfo? =
    try {
        taskInfo
    } catch (e: IllegalArgumentException) {
        null
    }

private fun ActivityManager.AppTask.bringToFront(): Boolean =
    try {
        moveToFront()
        true
    } catch (e: RuntimeException) {
        logger.w(e) { "Failed to move a task to the front" }
        false
    }

private val ActivityManager.RecentTaskInfo.isOAuth: Boolean
    get() = baseActivity?.className == OAuthRedirectActivity::class.java.name

@Suppress("DEPRECATION")
private val ActivityManager.RecentTaskInfo.stableId: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) taskId else persistentId
