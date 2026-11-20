package org.tasks.billing

import co.touchlab.kermit.Logger
import org.tasks.analytics.AnalyticsEvents
import org.tasks.compose.accounts.Platform
import org.tasks.compose.accounts.signInFollowsPaywall
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.tasksOrgAccount
import org.tasks.preferences.TasksPreferences

private const val TAG = "CloudOnboarding"

suspend fun TasksPreferences.paywallOpened(source: String) = set(
    TasksPreferences.deferCloudOnboarding,
    Platform.fromSource(source)?.signInFollowsPaywall == true,
)

suspend fun maybeTriggerCloudOnboarding(
    hasTasksSubscription: Boolean,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    logStep: (String) -> Unit,
) {
    val alreadySignedIn = caldavDao.tasksOrgAccount() != null
    val deferred = tasksPreferences.get(TasksPreferences.deferCloudOnboarding, false)
    Logger.d(tag = TAG) {
        "hasTasksSubscription=$hasTasksSubscription alreadySignedIn=$alreadySignedIn deferred=$deferred"
    }
    if (!hasTasksSubscription || alreadySignedIn) {
        return
    }
    if (deferred) {
        logStep(AnalyticsEvents.CloudOnboarding.DEFERRED)
        return
    }
    Logger.d(tag = TAG) { "setting needsCloudOnboarding=true" }
    logStep(AnalyticsEvents.CloudOnboarding.TRIGGERED)
    tasksPreferences.set(TasksPreferences.needsCloudOnboarding, true)
}
