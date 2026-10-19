package org.tasks.viewmodel

import org.tasks.billing.PurchaseTerms
import org.tasks.billing.SubscriptionProvider
import org.tasks.compose.settings.AccountData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProCardCreditTest {

    private fun supporter(terms: PurchaseTerms?) = SubscriptionProvider.SubscriptionInfo(
        sku = "annual_01",
        isMonthly = false,
        isTasksSubscription = false,
        terms = terms,
    )

    @Test
    fun aSupportersCreditIsWorthShowing() {
        assertEquals(
            "$9.00",
            creditToShow(
                sub = supporter(PurchaseTerms(creditCents = 900, creditCurrency = "USD")),
                accountData = null,
            ),
        )
    }

    @Test
    fun creditIsRenderedInTheCurrencyItIsHeldIn() {
        assertEquals(
            "9.00 EUR",
            creditToShow(
                sub = supporter(PurchaseTerms(creditCents = 900, creditCurrency = "EUR")),
                accountData = null,
            ),
        )
    }

    @Test
    fun anAccountsCreditIsWorthShowing() {
        assertEquals(
            "$9.00",
            creditToShow(
                sub = null,
                accountData = AccountData(
                    hasSubscription = true,
                    creditCents = 900,
                    creditCurrency = "USD",
                ),
            ),
        )
    }

    @Test
    fun creditWithoutASubscriptionIsNotTheCardsBusiness() {
        assertNull(
            creditToShow(
                sub = null,
                accountData = AccountData(creditCents = 900, creditCurrency = "USD"),
            )
        )
    }

    @Test
    fun aCurrencyWeWereNotToldIsWorseThanSayingNothing() {
        assertNull(
            creditToShow(
                sub = supporter(PurchaseTerms(creditCents = 900)),
                accountData = null,
            )
        )
    }

    @Test
    fun noCreditIsNothingToSay() {
        assertNull(creditToShow(sub = supporter(PurchaseTerms(creditCents = 0)), accountData = null))
        assertNull(creditToShow(sub = supporter(PurchaseTerms(status = "active")), accountData = null))
        assertNull(creditToShow(sub = supporter(null), accountData = null))
    }
}
