package org.tasks.billing

import android.app.Activity

class PlayBillingUnrecoverableException(message: String) : IllegalStateException(message)

class PlayBillingUnavailableException(message: String) : IllegalStateException(message)

fun interface PlayPurchaseVerifier {
    fun verify(purchase: PlayPurchase): Boolean
}

interface PlayBilling {
    suspend fun queryPurchases()

    suspend fun products(skus: List<String>): List<PlayProductInfo>

    suspend fun launchPurchaseFlow(
        activity: Activity,
        sku: String,
        subscription: Boolean = true,
        oldPurchaseToken: String? = null,
    )

    suspend fun acknowledge(purchase: PlayPurchase)

    suspend fun consume(purchase: PlayPurchase)

    suspend fun formattedPrice(sku: String): String? =
        products(listOf(sku)).firstOrNull()?.formattedPrice
}
