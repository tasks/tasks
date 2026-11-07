package org.tasks.compose.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import org.tasks.billing.PurchaseTerms

class StripeTabStateSaverTest {

    @Test
    fun aCodeKeepsTheAddressItWasSentTo() {
        assertEquals(
            StripeTabState.Code("alice@example.com"),
            roundTrip(StripeTabStateSaver, StripeTabState.Code("alice@example.com")),
        )
    }

    @Test
    fun aCodeKeepsTheRefusalBesideIt() {
        val refused = StripeTabState.Code("alice@example.com", Refusal.INVALID_CODE)

        assertEquals(refused, roundTrip(StripeTabStateSaver, refused))
    }

    @Test
    fun anInFlightCallComesBackAtTheStartOfItsStep() {
        assertEquals(
            StripeTabState.Email(),
            roundTrip(StripeTabStateSaver, StripeTabState.Email(busy = true)),
        )
        assertEquals(
            StripeTabState.Code("alice@example.com"),
            roundTrip(StripeTabStateSaver, StripeTabState.Code("alice@example.com", busy = true)),
        )
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

        assertEquals(ended, roundTrip(StripeTabStateSaver, ended))
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
            StripeTabState.Failed(Refusal.SERVER_FAILED, startOver = false),
        ).forEach { assertEquals(it, roundTrip(StripeTabStateSaver, it)) }
    }
}
