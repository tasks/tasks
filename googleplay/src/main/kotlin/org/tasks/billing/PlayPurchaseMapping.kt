package org.tasks.billing

import com.android.billingclient.api.Purchase

fun Purchase.toPlayPurchase() = PlayPurchase(
    sku = products.first(),
    purchaseToken = purchaseToken,
    originalJson = originalJson,
    signature = signature,
    isPurchased = purchaseState == Purchase.PurchaseState.PURCHASED,
    isAcknowledged = isAcknowledged,
    isAutoRenewing = isAutoRenewing,
)
