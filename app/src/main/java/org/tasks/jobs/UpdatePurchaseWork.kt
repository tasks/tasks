package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.injection.BaseWorker

@HiltWorker
class UpdatePurchaseWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val inventory: Inventory,
    private val billingClient: BillingClient,
) : BaseWorker(context, workerParams, firebase) {
    override suspend fun run(): Result {
        try {
            billingClient.queryPurchases(throwError = true)
        } catch (e: Exception) {
            return Result.retry()
        }
        inventory.purchases.values
            .filter { it.needsAcknowledgement }
            .takeIf { it.isNotEmpty() }
            ?.forEach { billingClient.acknowledge(it) }
            ?.apply { billingClient.queryPurchases() }
        return with(inventory.purchases.values) {
            when {
                any { it.needsAcknowledgement } -> Result.retry()
                all { it.isPurchased } -> Result.success()
                else -> Result.retry()
            }
        }
    }
}