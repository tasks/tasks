package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.billing.PlayBillingClient.Companion.preferredOffer

class PreferredOfferTest {
    private data class Offer(val id: String?, val free: Boolean = false)

    private fun List<Offer>.pick() = preferredOffer(offerId = { it.id }, startsFree = { it.free })

    @Test
    fun anEligibleTrialIsPreferredEvenWhenTheBasePlanIsListedFirst() {
        assertEquals(
            "trial",
            listOf(Offer(id = null), Offer(id = "trial", free = true)).pick()?.id,
        )
    }

    @Test
    fun withoutAnEligibleTrialTheBasePlanIsSold() {
        assertNull(listOf(Offer(id = "intro-discount"), Offer(id = null)).pick()?.id)
    }

    @Test
    fun anOfferIsStillSoldWhenThereIsNoBasePlan() {
        assertEquals("only", listOf(Offer(id = "only")).pick()?.id)
    }

    @Test
    fun noOffersMeansNothingToSell() {
        assertNull(emptyList<Offer>().pick())
    }
}
