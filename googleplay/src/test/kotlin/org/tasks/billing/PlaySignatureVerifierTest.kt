package org.tasks.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03_CANCELLED
import org.tasks.billing.PlayPurchaseFixtures.MONTHLY_01
import org.tasks.billing.PlayPurchaseFixtures.MONTHLY_03

@RunWith(RobolectricTestRunner::class)
class PlaySignatureVerifierTest {
    private val verifier = PlaySignatureVerifier()

    @Test
    fun aGenuinePlayPurchaseVerifies() {
        assertTrue(verifier.verify(MONTHLY_01.purchase))
        assertTrue(verifier.verify(MONTHLY_03.purchase))
        assertTrue(verifier.verify(ANNUAL_03.purchase))
        assertTrue(verifier.verify(ANNUAL_03_CANCELLED.purchase))
    }

    @Test
    fun aSignatureFromAnotherPurchaseIsRejected() {
        assertFalse(verifier.verify(MONTHLY_01.purchase.copy(signature = MONTHLY_03.signature)))
    }

    @Test
    fun aTamperedPayloadIsRejected() {
        val forged = MONTHLY_01.purchase.let {
            it.copy(originalJson = it.originalJson.replace("monthly_01", "monthly_03"))
        }

        assertFalse(verifier.verify(forged))
    }

    @Test
    fun missingDataIsRejected() {
        assertFalse(verifier.verify(MONTHLY_01.purchase.copy(signature = "")))
        assertFalse(verifier.verify(MONTHLY_01.purchase.copy(originalJson = "")))
    }

    @Test
    fun anUnusableBillingKeyRejectsEverything() {
        assertFalse(PlaySignatureVerifier(billingKey = "").verify(MONTHLY_01.purchase))
        assertFalse(PlaySignatureVerifier(billingKey = "not-a-key").verify(MONTHLY_01.purchase))
    }
}
