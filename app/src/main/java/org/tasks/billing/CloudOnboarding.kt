package org.tasks.billing

import org.tasks.analytics.AnalyticsEvents
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.preferences.TasksPreferences
import timber.log.Timber

suspend fun maybeTriggerCloudOnboarding(
    inventory: Inventory,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    logStep: (String) -> Unit,
) {
    val hasTasksSubscription = inventory.hasTasksSubscription
    val alreadySignedIn = caldavDao.getAccounts(TYPE_TASKS).isNotEmpty()
    Timber.d(
        "CloudOnboarding: onPurchased hasTasksSubscription=$hasTasksSubscription " +
                "alreadySignedIn=$alreadySignedIn"
    )
    if (hasTasksSubscription && !alreadySignedIn) {
        Timber.d("CloudOnboarding: setting needsCloudOnboarding=true")
        logStep(AnalyticsEvents.CloudOnboarding.TRIGGERED)
        tasksPreferences.set(TasksPreferences.needsCloudOnboarding, true)
    }
}
