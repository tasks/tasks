package org.tasks.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.analytics.Firebase
import org.tasks.analytics.logCloudOnboarding
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.ApplicationScope
import org.tasks.preferences.TasksPreferences
import javax.inject.Inject

class PaywallCompletion @Inject constructor(
    private val inventory: Inventory,
    private val caldavDao: CaldavDao,
    private val tasksPreferences: TasksPreferences,
    private val firebase: Firebase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun purchased() {
        scope.launch {
            inventory.updateTasksAccount()
            maybeTriggerCloudOnboarding(
                inventory.hasTasksSubscription,
                caldavDao,
                tasksPreferences,
                firebase::logCloudOnboarding,
            )
        }
    }

    fun signInFailed() {
        scope.launch { inventory.updateTasksAccount() }
    }
}
