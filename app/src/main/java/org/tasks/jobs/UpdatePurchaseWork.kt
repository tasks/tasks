package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PlayBilling
import org.tasks.billing.PlayBillingUnrecoverableException
import org.tasks.injection.BaseWorker
import timber.log.Timber

@HiltWorker
class UpdatePurchaseWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val inventory: Inventory,
    private val billing: PlayBilling,
) : BaseWorker(context, workerParams, firebase) {
    override suspend fun run(): Result {
        try {
            billing.queryPurchases()
        } catch (e: PlayBillingUnrecoverableException) {
            Timber.w(e)
            return Result.failure()
        } catch (e: Exception) {
            Timber.e(e)
            return Result.retry()
        }
        return with(inventory.purchases.values) {
            when {
                any { it.needsAcknowledgement } -> Result.retry()
                all { it.isPurchased } -> Result.success()
                else -> Result.retry()
            }
        }
    }
}