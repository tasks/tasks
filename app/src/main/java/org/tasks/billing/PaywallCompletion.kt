package org.tasks.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.injection.ApplicationScope
import javax.inject.Inject

class PaywallCompletion @Inject constructor(
    private val inventory: Inventory,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun updateTasksAccount() {
        scope.launch { inventory.updateTasksAccount() }
    }
}
