package org.tasks.billing

import android.app.Activity
import javax.inject.Inject

class NoOpPlayBilling @Inject constructor() : PlayBilling {
    override suspend fun queryPurchases() = Unit

    override suspend fun products(skus: List<String>): List<PlayProductInfo> = emptyList()

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        subscription: Boolean,
        oldPurchaseToken: String?,
    ) = Unit

    override suspend fun acknowledge(purchase: PlayPurchase) = Unit

    override suspend fun consume(purchase: PlayPurchase) = Unit
}
