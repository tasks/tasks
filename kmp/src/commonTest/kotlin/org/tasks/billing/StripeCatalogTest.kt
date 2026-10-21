package org.tasks.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StripeCatalogTest {
    private fun price(
        sku: String,
        cents: Int,
        interval: String = StripePrice.INTERVAL_YEAR,
        entitles: Boolean = false,
        trialDays: Int = 0,
    ) = StripePrice(
        priceId = "pri_$sku",
        sku = sku,
        amountCents = cents,
        interval = interval,
        entitles = entitles,
        trialDays = trialDays,
    )

    private fun ladder() = StripeCatalog(
        support = (1..10).map { dollars ->
            price("annual_" + dollars.toString().padStart(2, '0'), dollars * 100 - 1)
        },
    )

    @Test
    fun showsAnAnnualPriceAsAPerMonthFigure() {
        val annual = StripeCatalog.from(listOf(price("annual_30", 2999, entitles = true)))

        assertEquals("${'$'}2.50", annual.cloudAnnual?.formattedMonthlyPrice)
        assertEquals("${'$'}29.99", annual.cloudAnnual?.formattedPrice)
    }

    @Test
    fun aMonthlyPriceIsAlreadyPerMonth() {
        val monthly = StripeCatalog.from(
            listOf(price("monthly_03", 299, StripePrice.INTERVAL_MONTH, entitles = true))
        )

        assertEquals("${'$'}2.99", monthly.cloudMonthly?.formattedMonthlyPrice)
    }

    @Test
    fun theSliderStopsAtWhatStripeSells() {
        val catalog = StripeCatalog.from(listOf(price("annual_03", 299), price("annual_04", 399)))

        assertEquals(listOf("annual_03", "annual_04"), catalog.supporterRungs.map { it.sku })
    }

    @Test
    fun aMonthlyPriceAddsNoPositionOfItsOwn() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_12", 1199),
                price("monthly_01", 99, StripePrice.INTERVAL_MONTH),
            )
        )

        assertEquals(listOf("annual_12"), catalog.supporterRungs.map { it.sku })
    }

    @Test
    fun aTierBeyondTheUsualLadderStillGetsARung() {
        val catalog = StripeCatalog.from(listOf(price("annual_03", 299), price("annual_40", 3999)))

        assertEquals(listOf(299, 3999), catalog.supporterRungs.map { it.amountCents })
    }

    @Test
    fun ordersTheRungsCheapestFirst() {
        val catalog = StripeCatalog(
            support = listOf(price("annual_12", 1199), price("annual_03", 299)),
        )

        assertEquals(listOf(299, 1199), catalog.supporterRungs.map { it.amountCents })
    }

    @Test
    fun noSupporterTierMeansNoSliderAtAll() {
        val catalog = StripeCatalog.from(listOf(price("annual_30", 2999, entitles = true)))

        assertTrue(catalog.supporterRungs.isEmpty())
    }

    @Test
    fun carriesTheTrialTheServerNames() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_30", 3000, entitles = true, trialDays = 7),
                price("annual_01", 100),
            )
        )

        assertEquals(7, catalog.cloudAnnual?.trialDays)
        assertEquals(0, catalog.support.single().trialDays)
    }

    @Test
    fun keepsMonthlyAheadOfAnnualWithinATier() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_12", 1200),
                price("monthly_03", 300, StripePrice.INTERVAL_MONTH),
                price("annual_01", 100),
                price("monthly_01", 100, StripePrice.INTERVAL_MONTH),
            )
        )

        assertEquals(
            listOf("monthly_01", "monthly_03", "annual_01", "annual_12"),
            catalog.support.map { it.sku },
        )
    }

    @Test
    fun splitsTheLadderOnWhatTheServerSaysItUnlocks() {
        val catalog = StripeCatalog.from(
            listOf(
                price("monthly_01", 100, StripePrice.INTERVAL_MONTH),
                price("monthly_03", 300, StripePrice.INTERVAL_MONTH, entitles = true),
                price("annual_01", 100),
                price("annual_30", 3000, entitles = true),
            )
        )

        assertEquals(listOf("monthly_03", "annual_30"), catalog.cloud.map { it.sku })
        assertEquals(listOf("monthly_01", "annual_01"), catalog.support.map { it.sku })
    }

    @Test
    fun neverInfersEntitlementFromTheAmount() {
        val catalog = StripeCatalog.from(listOf(price("annual_30", 3000, entitles = false)))

        assertTrue(catalog.cloud.isEmpty())
        assertEquals(listOf("annual_30"), catalog.support.map { it.sku })
        assertNull(catalog.cloudAnnual)
    }

    @Test
    fun ordersEachListCheapestFirst() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_25", 2500),
                price("annual_01", 100),
                price("annual_12", 1200),
            )
        )

        assertEquals(listOf(100, 1200, 2500), catalog.support.map { it.amountCents })
    }

    @Test
    fun picksOneCloudRungPerBillingPeriod() {
        val catalog = StripeCatalog.from(
            listOf(
                price("monthly_03", 300, StripePrice.INTERVAL_MONTH, entitles = true),
                price("annual_30", 3000, entitles = true),
            )
        )

        assertEquals("monthly_03", catalog.cloudMonthly?.sku)
        assertEquals("annual_30", catalog.cloudAnnual?.sku)
    }

    @Test
    fun formatsTheCatalogPrice() {
        val catalog = StripeCatalog.from(
            listOf(
                price("monthly_03", 300, StripePrice.INTERVAL_MONTH, entitles = true),
                price("annual_30", 3000, entitles = true),
                price("annual_01", 100),
            )
        )

        assertEquals("$3.00", catalog.cloudMonthly?.formattedPrice)
        assertEquals("$30.00", catalog.cloudAnnual?.formattedPrice)
        assertEquals("$1.00", catalog.support.first().formattedPrice)
    }

    @Test
    fun namesTheCurrencyWhenItIsNotDollars() {
        assertEquals("3.00 EUR", formatAmount(300, "EUR"))
        assertEquals("0.99 EUR", formatAmount(99, "EUR"))
    }

    @Test
    fun findsAnOfferBySku() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_30", 3000, entitles = true),
                price("annual_01", 100),
            )
        )

        assertEquals(3000, catalog.price("annual_30")?.amountCents)
        assertEquals(100, catalog.price("annual_01")?.amountCents)
        assertNull(catalog.price("annual_99"))
        assertNull(catalog.price(null))
    }

    @Test
    fun anEmptyLadderSellsNothing() {
        assertTrue(StripeCatalog.from(emptyList()).isEmpty)
    }

    @Test
    fun readsTheRungTheSliderLandsOn() {
        val catalog = ladder()

        assertEquals("annual_01", catalog.supporterRungAt(0f)?.sku)
        assertEquals("annual_03", catalog.supporterRungAt(2f)?.sku)
        assertEquals("annual_10", catalog.supporterRungAt(9f)?.sku)
    }

    @Test
    fun aTickThatLandsJustShortStillReadsItsOwnRung() {
        val catalog = ladder()

        assertEquals("annual_03", catalog.supporterRungAt(1.9999999f)?.sku)
        assertEquals("annual_02", catalog.supporterRungAt(0.9999999f)?.sku)
        assertEquals("annual_04", catalog.supporterRungAt(3.0000002f)?.sku)
    }

    @Test
    fun theSliderStaysInsideTheLadder() {
        val catalog = ladder()

        assertEquals("annual_01", catalog.supporterRungAt(-1f)?.sku)
        assertEquals("annual_10", catalog.supporterRungAt(99f)?.sku)
    }

    @Test
    fun aSingleRungIsTheOnlyPlaceTheSliderCanLand() {
        val catalog = StripeCatalog(support = listOf(price("annual_01", 99)))

        assertEquals("annual_01", catalog.supporterRungAt(1f)?.sku)
    }

    @Test
    fun anEmptyLadderHasNoRungToLandOn() {
        assertNull(StripeCatalog().supporterRungAt(0f))
    }

    @Test
    fun opensOnTheRungMatchingTheSuggestedPrice() {
        val catalog = ladder()

        assertEquals(0, catalog.supporterRungFor(dollars = 1))
        assertEquals(2, catalog.supporterRungFor(dollars = 3))
        assertEquals(9, catalog.supporterRungFor(dollars = 10))
    }

    @Test
    fun aRungPricedAPennyUnderTheDollarIsStillThatDollar() {
        val catalog = ladder()

        assertEquals(299, catalog.supporterRungAt(catalog.supporterRungFor(3).toFloat())?.amountCents)
        assertEquals(999, catalog.supporterRungAt(catalog.supporterRungFor(10).toFloat())?.amountCents)
    }

    @Test
    fun aSuggestionAboveTheLadderOpensOnItsTopRung() {
        assertEquals(9, ladder().supporterRungFor(dollars = 50))
    }

    @Test
    fun anEmptyLadderHasNoRungToOpenOn() {
        assertEquals(-1, StripeCatalog().supporterRungFor(dollars = 10))
    }

    @Test
    fun noSuggestionOpensOnTheDefaultRung() {
        val catalog = ladder()

        assertEquals(
            catalog.supporterRungFor(dollars = DEFAULT_NAME_YOUR_PRICE),
            catalog.supporterRungFor(dollars = null),
        )
    }

    @Test
    fun pairsEachRungWithTheMonthlyPriceOfTheSameTier() {
        val catalog = StripeCatalog.from(
            listOf(
                price("annual_03", 299),
                price("monthly_03", 299, StripePrice.INTERVAL_MONTH),
                price("annual_05", 499),
            )
        )
        val rungs = catalog.supporterRungs

        assertEquals("monthly_03", catalog.supporterMonthlyFor(rungs[0])?.sku)
        assertNull(catalog.supporterMonthlyFor(rungs[1]))
    }
}
