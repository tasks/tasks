package org.tasks.billing

import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RenewalTest {
    private val period = currentTimeMillis() + 30 * ONE_DAY
    private val lastPeriod = currentTimeMillis() - 30 * ONE_DAY

    @Test
    fun aPaidPeriodRenews() {
        assertEquals(
            Renewal.Renews(period),
            PurchaseTerms(status = "active", expires = period).renewal(price = "$29.99"),
        )
    }

    @Test
    fun aCancelledSubscriptionEndsRatherThanRenews() {
        assertEquals(
            Renewal.Ends(period),
            PurchaseTerms(status = "active", expires = period, cancelAtPeriodEnd = true)
                .renewal(price = "$29.99"),
        )
    }

    @Test
    fun aSubscriptionStripeHasStoppedBillingEndsOnTheDayItPaidTo() {
        assertEquals(
            Renewal.Ends(period),
            PurchaseTerms(status = "canceled", expires = period).renewal(price = "$29.99"),
        )
    }

    @Test
    fun aDateAlreadyBehindThemHasEnded() {
        assertEquals(
            Renewal.Ended(lastPeriod),
            PurchaseTerms(status = "canceled", expires = lastPeriod).renewal(price = "$29.99"),
        )
    }

    @Test
    fun moneyThatWentBackLeavesNothingToHonour() {
        assertEquals(
            Renewal.Ended(lastPeriod),
            PurchaseTerms(status = "canceled", expires = lastPeriod, cancelAtPeriodEnd = true)
                .renewal(price = "$29.99"),
        )
    }

    @Test
    fun aChargeNeverCollectedNeverRenews() {
        listOf("incomplete", "incomplete_expired", "unpaid", "paused").forEach { status ->
            assertEquals(
                Renewal.Ended(lastPeriod),
                PurchaseTerms(status = status, expires = lastPeriod).renewal(price = "$29.99"),
            )
        }
    }

    @Test
    fun aStatusWeDoNotKnowIsNotReadAsLapsed() {
        assertEquals(
            Renewal.Renews(period),
            PurchaseTerms(status = "something_new", expires = period).renewal(price = "$29.99"),
        )
    }

    @Test
    fun aTrialSaysWhatFallsDueAndWhen() {
        assertEquals(
            Renewal.Trial(period, "$29.99"),
            PurchaseTerms(status = "trialing", trialEnd = period, expires = period)
                .renewal(price = "$29.99"),
        )
    }

    @Test
    fun aTrialSaysNothingRatherThanNameNoPrice() {
        assertNull(
            PurchaseTerms(status = "trialing", trialEnd = period, expires = period)
                .renewal(price = null)
        )
    }

    @Test
    fun aFailedPaymentIsSaidInsteadOfTheDate() {
        assertEquals(
            Renewal.PaymentFailed,
            PurchaseTerms(status = "past_due", expires = lastPeriod).renewal(price = "$29.99"),
        )
    }

    @Test
    fun aRowThatRecordsNoDatesSaysNothing() {
        assertNull(PurchaseTerms(creditCents = 900).renewal(price = "$29.99"))
    }

    private companion object {
        const val ONE_DAY = 24 * 60 * 60 * 1000L
    }
}
