package org.tasks.billing

import android.app.Activity
import android.content.Context
import org.tasks.analytics.Firebase
import org.tasks.jobs.WorkManager

@Suppress("UNUSED_PARAMETER")
class BillingClientImpl(
    context: Context,
    inventory: Inventory,
    firebase: Firebase,
    workManager: WorkManager
) : BillingClient {
    override suspend fun queryPurchases(throwError: Boolean) {}
    override suspend fun initiatePurchaseFlow(
            activity: Activity,
            sku: String,
            skuType: String,
            oldPurchase: Purchase?
    ) {}

    override suspend fun acknowledge(purchase: Purchase) {}
    override suspend fun getSkus(skus: List<String>): List<Sku> = emptyList()

    override suspend fun consume(sku: String) {}

    companion object {
        const val TYPE_SUBS = ""
    }
}