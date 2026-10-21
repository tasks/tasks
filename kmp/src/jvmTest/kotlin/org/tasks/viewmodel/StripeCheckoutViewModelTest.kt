package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.billing.StripeCheckout
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeCollection
import org.tasks.billing.StripeChange
import org.tasks.billing.PurchaseTerms
import org.tasks.billing.StripePlan
import org.tasks.billing.StripePrice
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.time.DateTimeUtils2
import org.tasks.viewmodel.StripeCheckoutViewModel.Notice
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.MAX_BACKOFF_MILLIS
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.POLL_INTERVAL_MILLIS
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.RATE_LIMITED_BACKOFF_MILLIS

@OptIn(ExperimentalCoroutinesApi::class)
class StripeCheckoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val offer = StripePrice("pri_30", "annual_30", 3000, entitles = true)
    private val supportOffer = StripePrice("pri_01", "annual_01", 100, entitles = false)

    private var prices: List<StripePrice>? = listOf(
        offer,
        StripePrice("pri_12", "annual_12", 1200, entitles = false),
        supportOffer,
    )
    private var plan: StripePlan? = null
    private var change: StripeChange = StripeChange.Changed(entitles = true)
    private var changes = mutableListOf<StripePrice>()
    private var requestedCheckouts = 0
    private var checkout: StripeCheckout =
        StripeCheckout.Started("c0de", "https://pay.stripe.com/x", expiresIn(CODE_LIFETIME))
    private val collections = ArrayDeque<StripeCollection>()
    private var polls = 0

    private val purchases = object : StripeCheckouts {
        override suspend fun prices() = prices
        override suspend fun currentPlan() = plan
        override suspend fun createCheckout(price: StripePrice?): StripeCheckout {
            requestedCheckouts++
            return checkout
        }
        override suspend fun changePlan(price: StripePrice): StripeChange {
            changes.add(price)
            if (change is StripeChange.Changed) {
                plan = StripePlan(price.sku)
            }
            return change
        }
        override suspend fun collectCheckout(code: String): StripeCollection {
            polls++
            return collections.removeFirstOrNull() ?: StripeCollection.Pending
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = StripeCheckoutViewModel(purchases)

    @Test
    fun loadsTheCatalogFromTheServer() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()

        assertEquals("annual_30", viewModel.viewState.value.catalog.cloudAnnual?.sku)
        assertEquals(
            listOf("annual_01", "annual_12"),
            viewModel.viewState.value.catalog.support.map { it.sku },
        )
    }

    @Test
    fun aCatalogTheServerWillNotGiveLeavesTheCardsLinkingOut() = runTest(dispatcher) {
        prices = null

        val viewModel = viewModel()
        runCurrent()

        assertTrue(viewModel.viewState.value.catalog.isEmpty)
    }

    @Test
    fun handsBackTheUrlToOpenInTheBrowser() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals("https://pay.stripe.com/x", viewModel.viewState.value.checkoutUrl)
        assertTrue(viewModel.viewState.value.busy)
    }

    @Test
    fun pollsEveryFiveSeconds() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 3 + 1)

        assertEquals(3, polls)
    }

    @Test
    fun aRateLimitedPollWaitsAsLongAsTheServerAsked() = runTest(dispatcher) {
        collections.add(StripeCollection.RateLimited(retryAfterMillis = 20_000L))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)
        assertEquals(1, polls)

        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        assertEquals(1, polls)

        advanceTimeBy(20_000L - POLL_INTERVAL_MILLIS * 3)
        assertEquals(2, polls)

        advanceTimeBy(POLL_INTERVAL_MILLIS)
        assertEquals(3, polls)
    }

    @Test
    fun aRateLimitWithNoAdviceStillBacksOff() = runTest(dispatcher) {
        collections.add(StripeCollection.RateLimited(retryAfterMillis = null))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 4 + 1)
        assertEquals(1, polls)

        advanceTimeBy(RATE_LIMITED_BACKOFF_MILLIS - POLL_INTERVAL_MILLIS * 3)
        assertEquals(2, polls)
    }

    @Test
    fun aServerAskingForTheRestOfTheHourIsNotObeyed() = runTest(dispatcher) {
        collections.add(StripeCollection.RateLimited(retryAfterMillis = 10 * 60 * 1000L))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)
        assertEquals(1, polls)

        advanceTimeBy(MAX_BACKOFF_MILLIS)
        assertEquals(2, polls)
    }

    @Test
    fun anOmittedExpiryStillStopsAfterAnHour() = runTest(dispatcher) {
        checkout = StripeCheckout.Started("c0de", "https://pay.stripe.com/x", 0L)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(CODE_LIFETIME + 1)

        assertEquals(Notice.Expired, viewModel.viewState.value.notice)
    }

    @Test
    fun aSecondPurchaseWaitsForTheFirstToFinish() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals("https://pay.stripe.com/x", viewModel.viewState.value.checkoutUrl)
        assertEquals(0, polls)
    }

    @Test
    fun aCollectedPurchaseEndsTheWaitWithNothingToSay() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.Restored))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertFalse(viewModel.viewState.value.busy)
        assertNull(viewModel.viewState.value.checkoutUrl)
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun readsTheCompletionOnlyOnce() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.Restored))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 5)

        assertEquals(1, polls)
    }

    @Test
    fun aSubscriptionBelowTheCloudFloorAsksForAnUpgrade() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.NeedsUpgrade))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(Notice.NeedsUpgrade, viewModel.viewState.value.notice)
    }

    @Test
    fun aPurchaseFundingAnotherAccountNamesIt() = runTest(dispatcher) {
        collections.add(
            StripeCollection.Collected(StripeRestoreOutcome.AccountInUse("b**@e**.com"))
        )
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(Notice.AccountInUse("b**@e**.com"), viewModel.viewState.value.notice)
    }

    @Test
    fun stopsPollingWhenTheCodeExpires() = runTest(dispatcher) {
        checkout = StripeCheckout.Started(
            "c0de",
            "https://pay.stripe.com/x",
            expiresIn(POLL_INTERVAL_MILLIS * 3 + 1_000),
        )
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 6)

        assertEquals(3, polls)
        assertEquals(Notice.Expired, viewModel.viewState.value.notice)
        assertNull(viewModel.viewState.value.checkoutUrl)
    }

    @Test
    fun anAlreadyCollectedCodeIsNotWorthWaitingOn() = runTest(dispatcher) {
        collections.add(StripeCollection.Gone)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(Notice.Expired, viewModel.viewState.value.notice)
        assertEquals(1, polls)
    }

    @Test
    fun cancellingStopsThePoll() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.cancel()
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)

        assertEquals(0, polls)
        assertFalse(viewModel.viewState.value.busy)
        assertNull(viewModel.viewState.value.checkoutUrl)
    }

    @Test
    fun aRefusedCheckoutNeverStartsPolling() = runTest(dispatcher) {
        checkout = StripeCheckout.RateLimited
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)

        assertEquals(0, polls)
        assertEquals(Notice.RateLimited, viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun mapsTheOtherCheckoutRefusals() = runTest(dispatcher) {
        listOf(
            StripeCheckout.RateLimited to Notice.RateLimited,
            StripeCheckout.Unavailable to Notice.Unavailable,
            StripeCheckout.UnknownPrice to Notice.Failed,
            StripeCheckout.Failed to Notice.Failed,
        ).forEach { (refusal, expected) ->
            checkout = refusal
            val viewModel = viewModel()
            runCurrent()

            viewModel.select(offer)
            runCurrent()

            assertEquals(expected, viewModel.viewState.value.notice)
        }
    }

    @Test
    fun someoneWhoAlreadyPaysChangesPlanInsteadOfBuying() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(listOf(offer), changes)
        assertEquals(0, requestedCheckouts)
        assertNull(viewModel.viewState.value.notice)
        assertEquals("annual_30", viewModel.viewState.value.plan?.sku)
    }

    @Test
    fun aChangeThatWentThroughAsksTheScreenToClose() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertTrue(viewModel.viewState.value.planChanged)

        viewModel.planChangeHandled()

        assertFalse(viewModel.viewState.value.planChanged)
    }

    @Test
    fun aChangeThatFailedLeavesTheScreenWhereItIs() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.Failed
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertFalse(viewModel.viewState.value.planChanged)
        assertEquals(Notice.Failed, viewModel.viewState.value.notice)
    }

    @Test
    fun aLadderThatCannotBeListedLeavesTheCatalogEmpty() = runTest(dispatcher) {
        prices = null
        val viewModel = viewModel()
        runCurrent()

        val state = viewModel.viewState.value
        assertFalse(state.loadingPrices)
        assertTrue(state.catalog.isEmpty)
    }

    @Test
    fun aStaleLadderStillSells() = runTest(dispatcher) {
        checkout = StripeCheckout.UnknownPrice
        val viewModel = viewModel()
        runCurrent()
        prices = null

        viewModel.select(offer)
        runCurrent()

        val state = viewModel.viewState.value
        assertEquals("annual_30", state.catalog.cloudAnnual?.sku)
    }

    @Test
    fun aChangeDuringATrialNamesTheDayTheChargeFallsDue() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_12",
            terms = PurchaseTerms(status = "trialing", trialEnd = 1760000000000L),
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1760000000000L, viewModel.viewState.value.confirmChange?.trialEnd)
    }

    @Test
    fun aChangeOnABilledSubscriptionHasNoTrialToTalkAbout() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_12",
            terms = PurchaseTerms(status = "active", trialEnd = 1760000000000L),
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertNull(viewModel.viewState.value.confirmChange?.trialEnd)
    }

    @Test
    fun aReceiptTheServerWontHonourPointsAtRestore() = runTest(dispatcher) {
        plan = StripePlan("annual_04")
        change = StripeChange.PurchaseGone
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(Notice.PurchaseGone, viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun aChangeWithNothingToProveItIsAClientBug() = runTest(dispatcher) {
        plan = StripePlan("annual_04")
        change = StripeChange.NoProofOfPurchase
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(Notice.Failed, viewModel.viewState.value.notice)
    }

    @Test
    fun aChangeOffTheCloudTierIsStillJustAChange() = runTest(dispatcher) {
        plan = StripePlan("annual_30")
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(supportOffer)
        runCurrent()

        assertEquals(supportOffer, viewModel.viewState.value.confirmChange?.price)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun aConfirmedDowngradeGoesThrough() = runTest(dispatcher) {
        plan = StripePlan("annual_30")
        change = StripeChange.Changed(entitles = false)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(supportOffer)
        runCurrent()

        viewModel.confirmChange()
        runCurrent()

        assertEquals(listOf(supportOffer), changes)
        assertNull(viewModel.viewState.value.notice)
        assertEquals("annual_01", viewModel.viewState.value.plan?.sku)
        assertNull(viewModel.viewState.value.confirmChange)
    }

    @Test
    fun aDismissedDowngradeChangesNothing() = runTest(dispatcher) {
        plan = StripePlan("annual_30")
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(supportOffer)
        runCurrent()

        viewModel.dismissChange()
        runCurrent()

        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.confirmChange)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun aCheckoutRefusedForAnExistingSubscriptionBecomesAChange() = runTest(dispatcher) {
        checkout = StripeCheckout.SubscriptionExists(product = "annual_01")
        plan = null
        val viewModel = viewModel()
        runCurrent()
        plan = StripePlan("annual_01")

        viewModel.select(offer)
        runCurrent()

        assertEquals(listOf(offer), changes)
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun aPlanThatTurnsOutNotToExistIsBoughtInstead() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.NotLinked
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals("https://pay.stripe.com/x", viewModel.viewState.value.checkoutUrl)
    }

    @Test
    fun aCancelledSubscriptionCannotBeRepriced() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.Cancelled
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(Notice.SubscriptionCancelled, viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun mapsTheOtherPlanChangeFailures() = runTest(dispatcher) {
        listOf(
            StripeChange.UnknownPrice to Notice.Failed,
            StripeChange.Unavailable to Notice.Unavailable,
            StripeChange.RateLimited to Notice.RateLimited,
            StripeChange.Failed to Notice.Failed,
        ).forEach { (refusal, expected) ->
            plan = StripePlan("annual_01")
            change = refusal
            val viewModel = viewModel()
            runCurrent()

            viewModel.select(offer)
            runCurrent()
            viewModel.confirmChange()
            runCurrent()

            assertEquals(expected, viewModel.viewState.value.notice)
            assertFalse(viewModel.viewState.value.busy)
        }
    }

    @Test
    fun theTierTheyAreAlreadyOnIsNotWorthACall() = runTest(dispatcher) {
        plan = StripePlan("annual_30")
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertTrue(changes.isEmpty())
        assertEquals(0, requestedCheckouts)
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun anUnrecognisedCurrentTierIsStillWarnedAbout() = runTest(dispatcher) {
        plan = StripePlan(null)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(supportOffer)
        runCurrent()

        assertEquals(supportOffer, viewModel.viewState.value.confirmChange?.price)
    }

    @Test
    fun aNoticeIsDismissedAndTheCardsAreUsableAgain() = runTest(dispatcher) {
        checkout = StripeCheckout.Failed
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.dismissNotice()

        assertNull(viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    private fun expiresIn(millis: Long) = DateTimeUtils2.currentTimeMillis() + millis

    private companion object {
        const val CODE_LIFETIME = 60 * 60 * 1000L
    }
}
