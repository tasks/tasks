package org.tasks.billing

import co.touchlab.kermit.Logger
import org.tasks.analytics.AnalyticsEvents
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

private const val TAG = "CloudOnboarding"

suspend fun maybeTriggerCloudOnboarding(
    hasTasksSubscription: Boolean,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    logStep: (String) -> Unit,
) {
    val alreadySignedIn = caldavDao.getAccounts(listOf(CaldavAccount.TYPE_TASKS)).isNotEmpty()
    Logger.d(tag = TAG) {
        "hasTasksSubscription=$hasTasksSubscription alreadySignedIn=$alreadySignedIn"
    }
    if (hasTasksSubscription && !alreadySignedIn) {
        Logger.d(tag = TAG) { "setting needsCloudOnboarding=true" }
        logStep(AnalyticsEvents.CloudOnboarding.TRIGGERED)
        tasksPreferences.set(TasksPreferences.needsCloudOnboarding, true)
    }
}
