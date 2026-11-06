package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositeSubscriptionProviderTest {
    @Test
    fun theStorePurchaseWinsWhenBothAreCloud() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(store), provider(restored))
        )

        assertEquals(store, composite.subscription.first())
    }

    @Test
    fun theStoresPurchaseTokenIsTheOneThatSurvives() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(store), provider(restored))
        )

        assertEquals("play-token-30", composite.subscription.first()?.purchaseToken)
    }

    @Test
    fun aCloudSubscriptionBeatsASupporterOneBoughtInTheStore() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(supporter), provider(restored))
        )

        val info = composite.subscription.first()
        assertEquals("annual_30", info?.sku)
        assertTrue(info!!.isTasksSubscription)
        assertTrue(info.isCardSubscription)
    }

    @Test
    fun theTermsOfACloudSubscriptionSurviveASupporterPurchase() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(supporter), provider(restored.copy(terms = terms)))
        )

        assertEquals(terms, composite.subscription.first()?.terms)
    }

    @Test
    fun aSupporterPurchaseIsStillReportedWhenNothingElseCovers() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(supporter), provider(null))
        )

        val info = composite.subscription.first()
        assertEquals("annual_10", info?.sku)
        assertFalse(info!!.isTasksSubscription)
    }

    @Test
    fun betweenTwoNonCloudSubscriptionsTheStoreOneWins() = runTest {
        val sponsor = SubscriptionProvider.SubscriptionInfo(
            sku = "desktop_play",
            isMonthly = false,
            isTasksSubscription = false,
            isGitHubSponsor = true,
        )
        val composite = CompositeSubscriptionProvider(
            listOf(provider(supporter), provider(sponsor))
        )

        assertEquals("play-token-10", composite.subscription.first()?.purchaseToken)
    }

    @Test
    fun aRestoredSubscriptionIsUsedWhenTheStoreHasNothing() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(null), provider(restored))
        )

        val info = composite.subscription.first()
        assertEquals(restored, info)
        assertNull(info?.purchaseToken)
    }

    @Test
    fun noProviderWithASubscriptionMeansNoSubscription() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(null), provider(null))
        )

        assertNull(composite.subscription.first())
    }

    @Test
    fun aLaterChangeIsPickedUp() = runTest {
        val storeFlow = MutableStateFlow<SubscriptionProvider.SubscriptionInfo?>(null)
        val composite = CompositeSubscriptionProvider(
            listOf(provider(storeFlow), provider(null))
        )
        assertNull(composite.subscription.first())

        storeFlow.value = store

        assertEquals(store, composite.subscription.first())
    }

    @Test
    fun nothingIsReportedUntilEveryProviderHas() = runTest {
        val cold = MutableSharedFlow<SubscriptionProvider.SubscriptionInfo?>()
        val composite = CompositeSubscriptionProvider(
            listOf(provider(store), provider(cold))
        )

        val outcome = withTimeoutOrNull(1_000) {
            composite.subscription.first()
            REPORTED
        }

        assertNull(outcome)
    }

    @Test
    fun aProviderReportingLateUnblocksTheRest() = runTest {
        val cold = MutableSharedFlow<SubscriptionProvider.SubscriptionInfo?>(replay = 1)
        val composite = CompositeSubscriptionProvider(
            listOf(provider(store), provider(cold))
        )
        cold.emit(null)

        val outcome = withTimeoutOrNull(1_000) {
            composite.subscription.first()
            REPORTED
        }

        assertEquals(REPORTED, outcome)
    }

    @Test
    fun aPriceIsTakenFromWhicheverProviderKnowsIt() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(null, price = null), provider(restored, price = "$30.00"))
        )

        assertEquals("$30.00", composite.getFormattedPrice("annual_30"))
    }

    @Test
    fun anUnknownPriceStaysUnknown() = runTest {
        val composite = CompositeSubscriptionProvider(
            listOf(provider(null, price = null), provider(null, price = null))
        )

        assertNull(composite.getFormattedPrice("annual_30"))
    }

    @Test
    fun syncWaitsOnEveryProviderThatVerifies() = runTest {
        assertTrue(
            CompositeSubscriptionProvider(
                listOf(provider(store, verified = true), provider(null, verified = true))
            ).awaitVerification()
        )
        assertFalse(
            CompositeSubscriptionProvider(
                listOf(provider(store, verified = true), provider(null, verified = false))
            ).awaitVerification()
        )
    }

    @Test
    fun oneProviderFindingATierIsEnough() = runTest {
        assertTrue(
            CompositeSubscriptionProvider(
                listOf(provider(null, tier = false), provider(null, tier = true))
            ).checkTier()
        )
        assertFalse(
            CompositeSubscriptionProvider(
                listOf(provider(null, tier = false), provider(null, tier = false))
            ).checkTier()
        )
    }

    @Test
    fun oneProviderGatingThirdPartySyncGatesItForAll() {
        assertTrue(
            CompositeSubscriptionProvider(
                listOf(provider(null, requiresPro = false), provider(null, requiresPro = true))
            ).googleAndMicrosoftRequirePro
        )
        assertFalse(
            CompositeSubscriptionProvider(
                listOf(provider(null, requiresPro = false), provider(null, requiresPro = false))
            ).googleAndMicrosoftRequirePro
        )
    }

    @Test
    fun thereHasToBeSomethingToCompose() {
        assertFailsWith<IllegalArgumentException> { CompositeSubscriptionProvider(emptyList()) }
    }

    private fun provider(
        info: SubscriptionProvider.SubscriptionInfo?,
        price: String? = null,
        verified: Boolean = true,
        tier: Boolean = false,
        requiresPro: Boolean = false,
    ) = provider(flowOf(info), price, verified, tier, requiresPro)

    private fun provider(
        infos: Flow<SubscriptionProvider.SubscriptionInfo?>,
        price: String? = null,
        verified: Boolean = true,
        tier: Boolean = false,
        requiresPro: Boolean = false,
    ) = object : SubscriptionProvider {
        override val subscription = infos
        override suspend fun getFormattedPrice(sku: String) = price
        override suspend fun awaitVerification() = verified
        override suspend fun checkTier() = tier
        override val googleAndMicrosoftRequirePro = requiresPro
    }

    companion object {
        private const val REPORTED = "reported"

        private val store = SubscriptionProvider.SubscriptionInfo(
            sku = "annual_30",
            isMonthly = false,
            isTasksSubscription = true,
            purchaseToken = "play-token-30",
        )

        private val supporter = SubscriptionProvider.SubscriptionInfo(
            sku = "annual_10",
            isMonthly = false,
            isTasksSubscription = false,
            purchaseToken = "play-token-10",
        )

        private val terms = PurchaseTerms(
            status = PurchaseTerms.STATUS_ACTIVE,
            expires = 1_900_000_000_000L,
        )

        private val restored = SubscriptionProvider.SubscriptionInfo(
            sku = "annual_30",
            isMonthly = false,
            isTasksSubscription = true,
            isCardSubscription = true,
        )
    }
}
