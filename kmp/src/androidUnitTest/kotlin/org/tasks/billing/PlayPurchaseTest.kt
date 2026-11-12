package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayPurchaseTest {
    @Test
    fun theOrderIdIsReadFromThePlayJson() {
        assertEquals(
            "GPA.3372-3222-8630-38485",
            purchase("""{"orderId":"GPA.3372-3222-8630-38485","productId":"annual_03"}""").orderId,
        )
    }

    @Test
    fun aPurchaseWithoutAnOrderIdHasNone() {
        assertNull(purchase("""{"productId":"annual_03"}""").orderId)
    }

    @Test
    fun unreadableJsonHasNoOrderId() {
        assertNull(purchase("{}").orderId)
        assertNull(purchase("not json").orderId)
    }

    private fun purchase(originalJson: String) = PlayPurchase(
        sku = "annual_03",
        purchaseToken = "token",
        originalJson = originalJson,
        signature = "signature",
        isPurchased = true,
        isAcknowledged = false,
        isAutoRenewing = true,
    )
}
