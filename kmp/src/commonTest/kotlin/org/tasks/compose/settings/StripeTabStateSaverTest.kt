package org.tasks.compose.settings

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import org.tasks.billing.PurchaseTerms

class StripeTabStateSaverTest {

    @Test
    fun aCodeKeepsTheAddressItWasSentTo() {
        assertEquals(
            StripeTabState.Code("alice@example.com"),
            roundTrip(StripeTabState.Code("alice@example.com")),
        )
    }

    @Test
    fun aCodeKeepsTheRefusalBesideIt() {
        assertEquals(
            StripeTabState.Code("alice@example.com", Refusal.INVALID_CODE),
            roundTrip(StripeTabState.Code("alice@example.com", Refusal.INVALID_CODE)),
        )
    }

    @Test
    fun anInFlightCallComesBackAtTheStartOfItsStep() {
        assertEquals(StripeTabState.Email(), roundTrip(StripeTabState.Busy))
    }

    @Test
    fun anEndedSubscriptionKeepsTheTermsItsNoteIsReadFrom() {
        val ended = StripeTabState.Ended(
            terms = PurchaseTerms(
                expires = 1762000000000,
                creditCents = 500,
                creditCurrency = "EUR",
            ),
        )

        assertEquals(ended, roundTrip(ended))
    }

    @Test
    fun everyOtherStepSurvivesUnchanged() {
        listOf(
            StripeTabState.Email(),
            StripeTabState.Email(Refusal.INVALID_EMAIL),
            StripeTabState.Restored(),
            StripeTabState.Restored(settling = true),
            StripeTabState.Upgrade,
            StripeTabState.AccountInUse("b**@e**.com"),
            StripeTabState.Ended(),
            StripeTabState.Failed(Refusal.TOO_MANY_REQUESTS, startOver = true),
            StripeTabState.Failed(Refusal.STORAGE_FAILED, startOver = false),
        ).forEach { assertEquals(it, roundTrip(it)) }
    }

    private fun roundTrip(state: StripeTabState): StripeTabState? {
        val saved = with(StripeTabStateSaver) { SaverScope { true }.save(state) }
        return StripeTabStateSaver.restore(saved!!)
    }
}
