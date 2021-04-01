package org.tasks.billing

import android.app.Activity
import android.content.Context
import org.tasks.analytics.Firebase

@Suppress("UNUSED_PARAMETER")
class BillingClientImpl(context: Context, inventory: Inventory, firebase: Firebase) : BillingClient {
    override suspend fun queryPurchases() {}
    override suspend fun initiatePurchaseFlow(
            activity: Activity,
            sku: String,
            skuType: String,
            oldPurchase: Purchase?
    ) {}

    override suspend fun consume(sku: String) {}

    companion object {
        const val TYPE_SUBS = ""
    }
}