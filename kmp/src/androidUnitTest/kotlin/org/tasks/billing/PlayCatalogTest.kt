package org.tasks.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.PlayCatalog.toBillingOption

class PlayCatalogTest {
    @Test
    fun catalogCoversTheCloudTiersAndTheSupporterLadder() {
        assertTrue(PlayCatalog.ALL_SKUS.contains(SubscriptionSkus.CLOUD_ANNUAL))
        assertTrue(PlayCatalog.ALL_SKUS.contains(SubscriptionSkus.CLOUD_MONTHLY))
        assertTrue(PlayCatalog.ALL_SKUS.containsAll((1..25).map { "annual_${"%02d".format(it)}" }))
        assertEquals(PlayCatalog.ALL_SKUS.size, PlayCatalog.ALL_SKUS.distinct().size)
    }

    @Test
    fun microsBecomeCents() {
        assertEquals(3000, product("annual_30", priceAmountMicros = 30_000_000).toBillingOption().amountCents)
    }

    @Test
    fun playsOwnFormattedPriceIsKept() {
        val price = product(
            "annual_30",
            formattedPrice = "€27,99",
            priceAmountMicros = 27_990_000,
            priceCurrencyCode = "EUR",
        ).toBillingOption()
        assertEquals("€27,99", price.formattedPrice)
        assertEquals("EUR", price.currency)
    }

    @Test
    fun aBlankFormattedPriceFallsBackToTheAmount() {
        val price = product("annual_30", formattedPrice = "", priceAmountMicros = 30_000_000)
            .toBillingOption()
        assertEquals("$30.00", price.formattedPrice)
    }

    @Test
    fun theSkuDecidesTheInterval() {
        assertTrue(product("monthly_03").toBillingOption().isMonthly)
        assertFalse(product("annual_30").toBillingOption().isMonthly)
    }

    @Test
    fun onlyTheCloudTiersEntitle() {
        assertTrue(product("annual_30").toBillingOption().entitles)
        assertTrue(product("monthly_03").toBillingOption().entitles)
        assertFalse(product("annual_10").toBillingOption().entitles)
        assertFalse(product("monthly_01").toBillingOption().entitles)
    }

    @Test
    fun trialPeriodsBecomeDays() {
        assertEquals(7, PlayCatalog.periodDays("P7D"))
        assertEquals(7, PlayCatalog.periodDays("P1W"))
        assertEquals(14, PlayCatalog.periodDays("P2W"))
        assertEquals(30, PlayCatalog.periodDays("P1M"))
        assertEquals(365, PlayCatalog.periodDays("P1Y"))
        assertEquals(0, PlayCatalog.periodDays(null))
        assertEquals(0, PlayCatalog.periodDays("nonsense"))
    }

    @Test
    fun theCatalogSplitsCloudFromSupporter() {
        val catalog = PlayCatalog.toCatalog(
            listOf(
                product("annual_30", priceAmountMicros = 30_000_000),
                product("monthly_03", priceAmountMicros = 3_000_000),
                product("annual_05", priceAmountMicros = 5_000_000),
                product("annual_10", priceAmountMicros = 10_000_000),
            )
        )
        assertEquals("annual_30", catalog.cloudAnnual?.sku)
        assertEquals("monthly_03", catalog.cloudMonthly?.sku)
        assertEquals(listOf("annual_05", "annual_10"), catalog.supporterRungs.map { it.sku })
    }

    private fun product(
        productId: String,
        formattedPrice: String = "$1.00",
        priceAmountMicros: Long = 1_000_000,
        priceCurrencyCode: String = "USD",
        trialPeriod: String? = null,
    ) = PlayProductInfo(
        productId = productId,
        formattedPrice = formattedPrice,
        priceAmountMicros = priceAmountMicros,
        priceCurrencyCode = priceCurrencyCode,
        trialPeriod = trialPeriod,
    )
}
