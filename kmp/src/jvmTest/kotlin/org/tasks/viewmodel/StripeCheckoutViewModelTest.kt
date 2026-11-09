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
import org.tasks.InMemoryDataStore
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.StripeCheckout
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeCollection
import org.tasks.billing.StripeChange
import org.tasks.billing.PurchaseTerms
import org.tasks.billing.StripePlan
import org.tasks.billing.BillingOption
import org.tasks.billing.ForeignCoverage
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.UnlockSource
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2
import org.tasks.viewmodel.StripeCheckoutViewModel.Notice
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.MAX_BACKOFF_MILLIS
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.MAX_UNANSWERED_POLLS
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.POLL_INTERVAL_MILLIS
import org.tasks.viewmodel.StripeCheckoutViewModel.Companion.RATE_LIMITED_BACKOFF_MILLIS

@OptIn(ExperimentalCoroutinesApi::class)
class StripeCheckoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val offer = BillingOption("pri_30", "annual_30", 3000, entitles = true)
    private val supportOffer = BillingOption("pri_01", "annual_01", 100, entitles = false)

    private var prices: List<BillingOption>? = listOf(
        offer,
        BillingOption("pri_12", "annual_12", 1200, entitles = false),
        supportOffer,
    )
    private var plan: StripePlan? = null
    private var knownCustomer = false
    private var coverage = ForeignCoverage()
    private var change: StripeChange = StripeChange.Changed(entitles = true)
    private var changes = mutableListOf<BillingOption>()
    private var requestedCheckouts = 0
    private var checkout: StripeCheckout =
        StripeCheckout.Started("c0de", "https://pay.stripe.com/x", expiresIn(CODE_LIFETIME))
    private val collections = ArrayDeque<StripeCollection>()
    private var collection: StripeCollection = StripeCollection.Pending
    private var polls = 0
    private var tierReads = 0
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()

    private val reporting = object : Reporting {
        override val tasksPreferences = TasksPreferences(InMemoryDataStore())
        override val analyticsEnabled = true
        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            events.add(event to params.toMap())
        }
        override fun addTask(source: String) = Unit
        override fun completeTask(source: String) = Unit
        override fun setSku(sku: String?) = Unit
        override fun reportException(t: Throwable, fatal: Boolean) = Unit
    }

    private val purchases = object : StripeCheckouts {
        override suspend fun prices() = prices
        override suspend fun currentPlan(refresh: Boolean): StripePlan? {
            if (refresh) tierReads++
            return plan
        }
        override suspend fun hasStripeCustomer() = knownCustomer
        override suspend fun coveredElsewhere() = coverage
        override suspend fun createCheckout(price: BillingOption?): StripeCheckout {
            requestedCheckouts++
            return checkout
        }
        override suspend fun changePlan(price: BillingOption): StripeChange {
            changes.add(price)
            if (change is StripeChange.Changed) {
                plan = StripePlan(price.sku)
            }
            return change
        }
        override suspend fun collectCheckout(code: String): StripeCollection {
            polls++
            return collections.removeFirstOrNull() ?: collection
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = StripeCheckoutViewModel(purchases, reporting)

    private fun logged(event: String) = events.filter { it.first == event }.map { it.second }

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
    fun aSubscriptionThatStoppedBillingIsSoldAgainAtTheSamePrice() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_30",
            terms = PurchaseTerms(status = "canceled", expires = 1760000000000L),
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
        assertEquals("https://pay.stripe.com/x", viewModel.viewState.value.checkoutUrl)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun aSubscriptionSetToCancelIsResumedAtTheSamePrice() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_30",
            terms = PurchaseTerms(status = "active", cancelAtPeriodEnd = true),
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(offer, viewModel.viewState.value.confirmChange?.price)

        viewModel.confirmChange()
        runCurrent()

        assertEquals(listOf(offer), changes)
        assertEquals(0, requestedCheckouts)
    }

    @Test
    fun thePriceAlreadyBilledIsNotWorthSelectingAgain() = runTest(dispatcher) {
        plan = StripePlan(sku = "annual_30", terms = PurchaseTerms(status = "active"))
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(0, requestedCheckouts)
        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.confirmChange)
    }

    @Test
    fun aChangeDoesNotReadTheTierBackTwice() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        val viewModel = viewModel()
        runCurrent()
        tierReads = 0

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(0, tierReads)
        assertEquals("annual_30", viewModel.viewState.value.plan?.sku)
    }

    @Test
    fun aServerThatStopsAnsweringBacksOffAndThenSaysSo() = runTest(dispatcher) {
        collection = StripeCollection.Unreachable
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 8 + 1)

        assertEquals(4, polls)
        assertNull(viewModel.viewState.value.notice)

        advanceTimeBy(MAX_BACKOFF_MILLIS * 4)

        assertEquals(MAX_UNANSWERED_POLLS, polls)
        assertEquals(Notice.Unavailable, viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun aPollThatAnswersAgainStartsTheBackoffOver() = runTest(dispatcher) {
        collections.add(StripeCollection.Unreachable)
        collections.add(StripeCollection.Unreachable)
        collections.add(StripeCollection.Pending)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 5 + 1)

        assertEquals(4, polls)
    }

    @Test
    fun aDeviceThatHasBoughtBeforeIsAKnownCustomer() = runTest(dispatcher) {
        knownCustomer = true
        val viewModel = viewModel()

        runCurrent()

        assertTrue(viewModel.viewState.value.knownCustomer)
    }

    @Test
    fun aSubscriptionCollectedAtTheTillIsNotRepriced() = runTest(dispatcher) {
        checkout = StripeCheckout.Collected(StripeRestoreOutcome.Restored)
        plan = null
        val viewModel = viewModel()
        runCurrent()
        plan = StripePlan("annual_30")

        viewModel.select(offer)
        runCurrent()

        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.notice)
        assertNull(viewModel.viewState.value.checkoutUrl)
        assertEquals("annual_30", viewModel.viewState.value.plan?.sku)
    }

    @Test
    fun aCollectedSubscriptionBelowTheFloorStillAsksForAnUpgrade() = runTest(dispatcher) {
        checkout = StripeCheckout.Collected(StripeRestoreOutcome.NeedsUpgrade)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(Notice.NeedsUpgrade, viewModel.viewState.value.notice)
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
    fun aChangeStripeCouldNotBeAskedAboutIsSentAgain() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.Unavailable
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(Notice.ChangeUnavailable, viewModel.viewState.value.notice)

        change = StripeChange.Changed(entitles = true)
        viewModel.retryChange()
        runCurrent()

        assertEquals(listOf(offer, offer), changes)
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun aRefusedCardIsToldApartFromAnOutage() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.CardDeclined
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertEquals(Notice.CardDeclined, viewModel.viewState.value.notice)

        change = StripeChange.Changed(entitles = true)
        viewModel.retryChange()
        runCurrent()

        assertEquals(listOf(offer, offer), changes)
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun aDismissedRetryableNoticeLeavesNothingToRetry() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.Unavailable
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()
        viewModel.dismissNotice()
        viewModel.retryChange()
        runCurrent()

        assertEquals(listOf(offer), changes)
    }

    @Test
    fun aChangeThatTookBackACancellationLeavesTheScreenLikeAnyOtherChange() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.Changed(entitles = true, resumed = true)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()
        viewModel.confirmChange()
        runCurrent()

        assertNull(viewModel.viewState.value.notice)
        assertTrue(viewModel.viewState.value.planChanged)
    }

    @Test
    fun aCloudTierIsSoldToAnAccountAnotherProviderPaysForSoTheyCanMoveToACard() =
        runTest(dispatcher) {
            coverage = ForeignCoverage(covered = true, source = UnlockSource.GOOGLE_PLAY)
            val viewModel = viewModel()
            runCurrent()

            viewModel.select(offer)
            runCurrent()

            assertEquals(1, requestedCheckouts)
            assertNull(viewModel.viewState.value.notice)
        }

    @Test
    fun theProviderThatPaysIsCarriedForTheWarningTheCardShows() = runTest(dispatcher) {
        coverage = ForeignCoverage(covered = true, source = UnlockSource.GOOGLE_PLAY)
        val viewModel = viewModel()
        runCurrent()

        assertTrue(viewModel.viewState.value.coveredElsewhere)
        assertEquals(UnlockSource.GOOGLE_PLAY, viewModel.viewState.value.coveredBy)
    }

    @Test
    fun aSupporterRungIsStillSoldToAnAccountAnotherProviderPaysFor() = runTest(dispatcher) {
        coverage = ForeignCoverage(covered = true, source = UnlockSource.GOOGLE_PLAY)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(supportOffer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
    }

    @Test
    fun aCustomerOnAnotherAccountIsNotAPlanChange() = runTest(dispatcher) {
        checkout = StripeCheckout.AccountInUse("a***@example.com")
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(Notice.AccountInUse("a***@example.com"), viewModel.viewState.value.notice)
        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.busySku)
    }

    @Test
    fun anAccountLinkedToAnotherCustomerIsNotSentRoundTheHouses() = runTest(dispatcher) {
        checkout = StripeCheckout.AccountLinkConflict
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(Notice.AccountLinkConflict, viewModel.viewState.value.notice)
        assertTrue(changes.isEmpty())
        assertEquals(1, requestedCheckouts)
    }

    @Test
    fun aRefusalOnlyARestoreCanRepairOffersOne() = runTest(dispatcher) {
        checkout = StripeCheckout.RestoreRequired
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(Notice.RestoreRequired, viewModel.viewState.value.notice)
        assertFalse(viewModel.viewState.value.busy)
    }

    @Test
    fun aRefusedSaleTheServerThenKnowsNoSubscriptionForStopsThere() = runTest(dispatcher) {
        checkout = StripeCheckout.SubscriptionExists(product = "annual_01")
        change = StripeChange.NotLinked
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
        assertEquals(listOf(offer), changes)
        assertEquals(Notice.Failed, viewModel.viewState.value.notice)
    }

    @Test
    fun aSubscriptionThatHasStoppedCoveringIsSoldAgainRatherThanRepriced() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_01",
            terms = PurchaseTerms(status = "canceled"),
            covered = false,
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.confirmChange)
    }

    @Test
    fun aCancelledCloudRowInsideItsGraceIsStillSoldTo() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "monthly_03",
            terms = PurchaseTerms(status = "canceled"),
            covered = true,
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun aSupporterRungStripeHasStoppedBillingIsSoldTo() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_01",
            terms = PurchaseTerms(status = "canceled"),
            covered = true,
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(1, requestedCheckouts)
        assertTrue(changes.isEmpty())
        assertNull(viewModel.viewState.value.notice)
    }

    @Test
    fun aSubscriptionCancelledForTheEndOfItsPeriodIsRepriced() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_01",
            terms = PurchaseTerms(status = "active", cancelAtPeriodEnd = true),
            covered = true,
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(offer, viewModel.viewState.value.confirmChange?.price)
        assertEquals(0, requestedCheckouts)
    }

    @Test
    fun aSubscriptionStripeIsStillBillingIsRepriced() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_01",
            terms = PurchaseTerms(status = "past_due"),
            covered = true,
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(offer, viewModel.viewState.value.confirmChange?.price)
        assertEquals(0, requestedCheckouts)
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

    @Test
    fun aCheckoutThatReachedTheBrowserIsLogged() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(
            listOf(
                mapOf(
                    AnalyticsEvents.PARAM_SKU to "annual_30",
                    AnalyticsEvents.PARAM_PERIOD to AnalyticsEvents.PERIOD_ANNUAL,
                )
            ),
            logged(AnalyticsEvents.CHECKOUT_STARTED),
        )
    }

    @Test
    fun aMonthlyPriceIsLoggedAsMonthly() = runTest(dispatcher) {
        val monthly = BillingOption(
            priceId = "pri_m",
            sku = "monthly_03",
            amountCents = 299,
            interval = BillingOption.INTERVAL_MONTH,
            entitles = true,
        )
        prices = listOf(monthly)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(monthly)
        runCurrent()

        assertEquals(
            AnalyticsEvents.PERIOD_MONTHLY,
            logged(AnalyticsEvents.CHECKOUT_STARTED).single()[AnalyticsEvents.PARAM_PERIOD],
        )
    }

    @Test
    fun everyCheckoutRefusalIsLoggedWithItsReason() = runTest(dispatcher) {
        listOf(
            StripeCheckout.RateLimited to AnalyticsEvents.Checkout.RATE_LIMITED,
            StripeCheckout.Unavailable to AnalyticsEvents.Checkout.UNAVAILABLE,
            StripeCheckout.UnknownPrice to AnalyticsEvents.Checkout.UNKNOWN_PRICE,
            StripeCheckout.Failed to AnalyticsEvents.Checkout.FAILED,
            StripeCheckout.RestoreRequired to AnalyticsEvents.Checkout.RESTORE_REQUIRED,
            StripeCheckout.AccountLinkConflict to AnalyticsEvents.Checkout.ACCOUNT_LINK_CONFLICT,
            StripeCheckout.AccountInUse("b**@e**.com") to AnalyticsEvents.Checkout.ACCOUNT_IN_USE,
        ).forEach { (refusal, reason) ->
            events.clear()
            checkout = refusal
            val viewModel = viewModel()
            runCurrent()

            viewModel.select(offer)
            runCurrent()

            assertEquals(
                mapOf(
                    AnalyticsEvents.PARAM_SKU to "annual_30",
                    AnalyticsEvents.PARAM_REASON to reason,
                ),
                logged(AnalyticsEvents.CHECKOUT_ERROR).single(),
            )
            assertTrue(logged(AnalyticsEvents.CHECKOUT_STARTED).isEmpty())
        }
    }

    @Test
    fun aCollectedCheckoutIsLoggedWithWhatItLeftBehind() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.Restored))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(
            listOf(
                mapOf(
                    AnalyticsEvents.PARAM_SKU to "annual_30",
                    AnalyticsEvents.PARAM_RESULT to AnalyticsEvents.Checkout.RESTORED,
                )
            ),
            logged(AnalyticsEvents.CHECKOUT_COLLECTED),
        )
    }

    @Test
    fun aCollectionBelowTheCloudFloorIsLoggedAsNeedingAnUpgrade() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.NeedsUpgrade))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(
            AnalyticsEvents.Checkout.NEEDS_UPGRADE,
            logged(AnalyticsEvents.CHECKOUT_COLLECTED).single()[AnalyticsEvents.PARAM_RESULT],
        )
    }

    @Test
    fun aReceiptThatWouldNotEncryptIsLoggedAsSuch() = runTest(dispatcher) {
        collections.add(
            StripeCollection.Collected(StripeRestoreOutcome.StorageFailed(encryptionFailed = true))
        )
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(
            AnalyticsEvents.Checkout.ENCRYPTION_FAILED,
            logged(AnalyticsEvents.CHECKOUT_COLLECTED).single()[AnalyticsEvents.PARAM_RESULT],
        )
    }

    @Test
    fun aSubscriptionCollectedAtTheTillIsLoggedWithoutAStart() = runTest(dispatcher) {
        checkout = StripeCheckout.Collected(StripeRestoreOutcome.Restored)
        val viewModel = viewModel()
        runCurrent()

        viewModel.select(offer)
        runCurrent()

        assertEquals(
            AnalyticsEvents.Checkout.RESTORED,
            logged(AnalyticsEvents.CHECKOUT_COLLECTED).single()[AnalyticsEvents.PARAM_RESULT],
        )
        assertTrue(logged(AnalyticsEvents.CHECKOUT_STARTED).isEmpty())
    }

    @Test
    fun aCodeThatRanOutIsLoggedAsExpired() = runTest(dispatcher) {
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

        assertEquals(
            AnalyticsEvents.Checkout.EXPIRED,
            logged(AnalyticsEvents.CHECKOUT_ERROR).single()[AnalyticsEvents.PARAM_REASON],
        )
    }

    @Test
    fun aCodeSomethingElseAlreadyCollectedIsLoggedAsGone() = runTest(dispatcher) {
        collections.add(StripeCollection.Gone)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(
            AnalyticsEvents.Checkout.GONE,
            logged(AnalyticsEvents.CHECKOUT_ERROR).single()[AnalyticsEvents.PARAM_REASON],
        )
    }

    @Test
    fun aSubscriptionTheServerWontHonourIsLoggedAsInvalid() = runTest(dispatcher) {
        collections.add(StripeCollection.SubscriptionInvalid)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(
            AnalyticsEvents.Checkout.SUBSCRIPTION_INVALID,
            logged(AnalyticsEvents.CHECKOUT_ERROR).single()[AnalyticsEvents.PARAM_REASON],
        )
    }

    @Test
    fun aServerThatStopsAnsweringIsLoggedAsUnreachable() = runTest(dispatcher) {
        collection = StripeCollection.Unreachable
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 8 + 1)

        assertTrue(logged(AnalyticsEvents.CHECKOUT_ERROR).isEmpty())

        advanceTimeBy(MAX_BACKOFF_MILLIS * 4)

        assertEquals(
            AnalyticsEvents.Checkout.UNREACHABLE,
            logged(AnalyticsEvents.CHECKOUT_ERROR).single()[AnalyticsEvents.PARAM_REASON],
        )
    }

    @Test
    fun walkingAwayFromTheBrowserIsLoggedAsAbandoned() = runTest(dispatcher) {
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.cancel()

        assertEquals(
            listOf(mapOf(AnalyticsEvents.PARAM_SKU to "annual_30")),
            logged(AnalyticsEvents.CHECKOUT_ABANDONED),
        )
    }

    @Test
    fun aCheckoutThatAlreadySettledIsNotLoggedAsAbandoned() = runTest(dispatcher) {
        collections.add(StripeCollection.Collected(StripeRestoreOutcome.Restored))
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        viewModel.cancel()

        assertTrue(logged(AnalyticsEvents.CHECKOUT_ABANDONED).isEmpty())
    }

    @Test
    fun aPlanChangeIsLoggedWithItsResult() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.confirmChange()
        runCurrent()

        assertEquals(
            listOf(
                mapOf(
                    AnalyticsEvents.PARAM_SKU to "annual_30",
                    AnalyticsEvents.PARAM_PERIOD to AnalyticsEvents.PERIOD_ANNUAL,
                    AnalyticsEvents.PARAM_RESULT to AnalyticsEvents.Checkout.CHANGED,
                )
            ),
            logged(AnalyticsEvents.PLAN_CHANGE),
        )
    }

    @Test
    fun aResumedSubscriptionIsLoggedAsResumed() = runTest(dispatcher) {
        plan = StripePlan(
            sku = "annual_30",
            terms = PurchaseTerms(status = "active", cancelAtPeriodEnd = true),
        )
        change = StripeChange.Changed(entitles = true, resumed = true)
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.confirmChange()
        runCurrent()

        assertEquals(
            AnalyticsEvents.Checkout.RESUMED,
            logged(AnalyticsEvents.PLAN_CHANGE).single()[AnalyticsEvents.PARAM_RESULT],
        )
    }

    @Test
    fun aDeclinedCardIsLogged() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.CardDeclined
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.confirmChange()
        runCurrent()

        assertEquals(
            AnalyticsEvents.Checkout.CARD_DECLINED,
            logged(AnalyticsEvents.PLAN_CHANGE).single()[AnalyticsEvents.PARAM_RESULT],
        )
    }

    @Test
    fun aDismissedPlanChangeIsLogged() = runTest(dispatcher) {
        plan = StripePlan("annual_30")
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(supportOffer)
        runCurrent()

        viewModel.dismissChange()

        assertEquals(
            listOf(
                mapOf(
                    AnalyticsEvents.PARAM_SKU to "annual_01",
                    AnalyticsEvents.PARAM_PERIOD to AnalyticsEvents.PERIOD_ANNUAL,
                    AnalyticsEvents.PARAM_RESULT to AnalyticsEvents.Checkout.DISMISSED,
                )
            ),
            logged(AnalyticsEvents.PLAN_CHANGE),
        )
    }

    @Test
    fun aPlanThatTurnsOutNotToExistIsLoggedAndThenSold() = runTest(dispatcher) {
        plan = StripePlan("annual_01")
        change = StripeChange.NotLinked
        val viewModel = viewModel()
        runCurrent()
        viewModel.select(offer)
        runCurrent()

        viewModel.confirmChange()
        runCurrent()

        assertEquals(
            AnalyticsEvents.Checkout.NOT_LINKED,
            logged(AnalyticsEvents.PLAN_CHANGE).single()[AnalyticsEvents.PARAM_RESULT],
        )
        assertEquals(1, logged(AnalyticsEvents.CHECKOUT_STARTED).size)
    }

    private fun expiresIn(millis: Long) = DateTimeUtils2.currentTimeMillis() + millis

    private companion object {
        const val CODE_LIFETIME = 60 * 60 * 1000L
    }
}
