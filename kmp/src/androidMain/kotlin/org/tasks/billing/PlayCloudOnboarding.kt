package org.tasks.billing

import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.TasksPreferences

suspend fun maybeTriggerCloudOnboarding(
    purchased: List<PlayPurchase>,
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    logStep: (String) -> Unit,
) = maybeTriggerCloudOnboarding(
    hasTasksSubscription = purchased.any { it.isPurchased && it.isTasksSubscription },
    caldavDao = caldavDao,
    tasksPreferences = tasksPreferences,
    logStep = logStep,
)
