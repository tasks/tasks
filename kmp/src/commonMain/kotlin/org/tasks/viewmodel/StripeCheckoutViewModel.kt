package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.AnalyticsEvents.BillingFlow
import org.tasks.analytics.analyticsName
import org.tasks.analytics.logBillingFlow
import org.tasks.analytics.Reporting
import org.tasks.billing.analyticsResult
import org.tasks.billing.logStripeRestore
import org.tasks.billing.BillingCatalog
import org.tasks.billing.StripeChange
import org.tasks.billing.StripeCheckout
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeCollection
import org.tasks.billing.BillingOption
import org.tasks.billing.BillingProvider
import org.tasks.billing.StripePlan
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.UnlockSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private const val TAG = "StripeCheckout"

open class StripeCheckoutViewModel(
    private val purchases: StripeCheckouts,
    private val reporting: Reporting,
    private val background: CoroutineScope,
) : ViewModel() {

    data class ViewState(
        val catalog: BillingCatalog = BillingCatalog(),
        val loadingPrices: Boolean = true,
        val plan: StripePlan? = null,
        val loadingPlan: Boolean = true,
        val busySku: String? = null,
        val checkoutUrl: String? = null,
        val confirmChange: PendingChange? = null,
        val planChanged: Boolean = false,
        val knownCustomer: Boolean? = null,
        val coveredElsewhere: Boolean = false,
        val coveredBy: UnlockSource? = null,
        val notice: Notice? = null,
    ) {
        val busy: Boolean get() = busySku != null
    }

    data class PendingChange(
        val price: BillingOption,
        val trialEnd: Long? = null,
    )

    sealed interface Notice {
        data object NeedsUpgrade : Notice

        data object PurchaseGone : Notice

        data class AccountInUse(val existingAccount: String) : Notice

        data object Expired : Notice

        data object SubscriptionInvalid : Notice

        data object SubscriptionCancelled : Notice

        data object CardDeclined : Notice

        data object ChangeUnavailable : Notice

        data object RestoreRequired : Notice

        data object AccountLinkConflict : Notice

        data object EncryptionFailed : Notice

        data object RateLimited : Notice

        data object Unavailable : Notice

        data object Failed : Notice
    }

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private var pollJob: Job? = null

    private class Attempt(val sku: String) {
        var code: String = ""
        var expiresAt: Long = 0L
        var launched = false
        var outcome = Outcome.OPEN
        var watched = false
        var inFlight = 0
        var endWhenReturned: (() -> Unit)? = null
    }

    private enum class Outcome { OPEN, FAILED, PAID }

    private val attempts = mutableListOf<Attempt>()

    private var checkout: Attempt? = null

    private var changing: Attempt? = null

    private var quiet = false

    private var retryable: BillingOption? = null

    init {
        viewModelScope.launch {
            val knownCustomer = purchases.hasStripeCustomer()
            _viewState.update { it.copy(knownCustomer = knownCustomer) }
        }
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch { refreshPlan() }
    }

    fun select(price: BillingOption) {
        if (_viewState.value.busy || _viewState.value.confirmChange != null) return
        val plan = _viewState.value.plan
        when {
            plan == null || !plan.canChange -> purchase(price)
            plan.sku == price.sku && !plan.resumable -> return
            else -> {
                changing = startAttempt(price)
                _viewState.update {
                    it.copy(
                        confirmChange = PendingChange(
                            price = price,
                            trialEnd = plan.trialEnd,
                        )
                    )
                }
            }
        }
    }

    fun confirmChange() {
        val pending = _viewState.value.confirmChange ?: return
        val attempt = changing?.takeIf { it.sku == pending.price.sku } ?: startAttempt(pending.price)
        changing = null
        _viewState.update { it.copy(confirmChange = null) }
        change(pending.price, attempt)
    }

    fun dismissChange() {
        changing?.let { end(it, AnalyticsEvents.Checkout.USER_CANCELED) }
        changing = null
        _viewState.update { it.copy(confirmChange = null) }
    }

    fun cancel() {
        val left = checkout
        pollJob?.cancel()
        pollJob = null
        checkout = null
        _viewState.update { it.copy(busySku = null, checkoutUrl = null) }
        left?.let { abandon(it, AnalyticsEvents.Checkout.USER_CANCELED) }
    }

    fun browserClosed() {
        val started = checkout ?: return
        if (_viewState.value.checkoutUrl == null) return
        poll(started, firstWait = 0L, onUnsettled = ::leaveQuietly)
    }

    private fun leaveQuietly() {
        if (quiet) return
        quiet = true
        checkout?.let { end(it, AnalyticsEvents.Checkout.BROWSER_CLOSED) }
        _viewState.update { it.copy(busySku = null, checkoutUrl = null) }
    }

    override fun onCleared() {
        attempts.toList().forEach { abandon(it, AnalyticsEvents.Checkout.USER_CANCELED) }
        super.onCleared()
    }

    fun reopenCheckout(): String? {
        val url = _viewState.value.checkoutUrl ?: return null
        reporting.logEvent(
            AnalyticsEvents.PRICING_REOPEN_CHECKOUT,
            AnalyticsEvents.PARAM_PROVIDER to BillingProvider.STRIPE.analyticsName,
            AnalyticsEvents.PARAM_SKU to checkout?.sku.orEmpty(),
        )
        return url
    }

    fun portalOpened() {
        reporting.logEvent(
            AnalyticsEvents.PRICING_OPEN_PORTAL,
            AnalyticsEvents.PARAM_PROVIDER to BillingProvider.STRIPE.analyticsName,
            AnalyticsEvents.PARAM_SKU to retryable?.sku.orEmpty(),
        )
    }

    fun dismissNotice() {
        retryable = null
        _viewState.update { it.copy(notice = null) }
    }

    fun retryChange() {
        val price = retryable ?: return
        retryable = null
        _viewState.update { it.copy(notice = null) }
        change(price, startAttempt(price))
    }

    fun planChangeHandled() = _viewState.update { it.copy(planChanged = false) }

    private fun purchase(price: BillingOption, launched: Attempt? = null) {
        lingerIfLeft()
        pollJob?.cancel()
        pollJob = null
        checkout = null
        quiet = false
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch {
            val attempt = launched ?: Attempt(price.sku).also { attempts.add(it) }
            val checkout = attempt.guarded {
                purchases.createCheckout(price).also { checkout ->
                    checkout.errorReason?.let { fail(attempt, it) }
                    if (checkout is StripeCheckout.Collected) restored(attempt, checkout.outcome)
                }
            }
            when (checkout) {
                is StripeCheckout.Started -> {
                    if (!attempt.launched) markLaunched(attempt)
                    attempt.code = checkout.code
                    attempt.expiresAt = checkout.expiresAt
                    this@StripeCheckoutViewModel.checkout = attempt
                    _viewState.update { it.copy(checkoutUrl = checkout.url) }
                    poll(attempt)
                }
                is StripeCheckout.AccountInUse ->
                    finish(Notice.AccountInUse(checkout.existingAccount))
                is StripeCheckout.Collected -> {
                    refreshPlan()
                    finish(checkout.outcome.notice)
                }
                is StripeCheckout.SubscriptionExists -> {
                    Logger.w(TAG) { "Checkout refused: this account already pays for ${checkout.product}" }
                    refreshPlan()
                    if (!attempt.launched) markLaunched(attempt)
                    changeNow(price, attempt, sellInstead = false)
                }
                StripeCheckout.AccountLinkConflict -> {
                    Logger.e(TAG) { "Checkout refused: the account holds another customer" }
                    finish(Notice.AccountLinkConflict)
                }
                StripeCheckout.RestoreRequired -> finish(Notice.RestoreRequired)
                StripeCheckout.RateLimited -> finish(Notice.RateLimited)
                StripeCheckout.Unavailable -> finish(Notice.Unavailable)
                StripeCheckout.UnknownPrice -> {
                    Logger.e(TAG) { "The server does not sell ${price.sku}" }
                    loadCatalog()
                    finish(Notice.Failed)
                }
                StripeCheckout.Failed -> finish(Notice.Failed)
            }
        }
    }

    private fun change(price: BillingOption, attempt: Attempt) {
        lingerIfLeft()
        pollJob?.cancel()
        pollJob = null
        checkout = null
        quiet = false
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch { changeNow(price, attempt) }
    }

    private suspend fun changeNow(
        price: BillingOption,
        attempt: Attempt,
        sellInstead: Boolean = true,
    ) {
        val result = attempt.guarded {
            purchases.changePlan(price).also { result ->
                when {
                    result is StripeChange.Changed ->
                        terminal(attempt, BillingFlow.PURCHASED, result.resultName)
                    result != StripeChange.NotLinked || !sellInstead ->
                        end(attempt, result.resultName)
                }
            }
        }
        when (result) {
            is StripeChange.Changed -> {
                refreshPlan(refresh = false)
                finish(null)
                _viewState.update { it.copy(planChanged = true) }
            }
            StripeChange.NotLinked -> if (sellInstead) {
                _viewState.update { it.copy(plan = null) }
                purchase(price, launched = attempt)
            } else {
                Logger.e(TAG) { "The server refused the sale and then knew no subscription" }
                finish(Notice.Failed)
            }
            StripeChange.Cancelled -> finish(Notice.SubscriptionCancelled)
            StripeChange.PurchaseGone -> finish(Notice.PurchaseGone)
            StripeChange.NoProofOfPurchase -> {
                Logger.e(TAG) { "Plan change refused: no proof of purchase the server would take" }
                refreshPlan()
                finish(Notice.Failed)
            }
            StripeChange.RateLimited -> finish(Notice.RateLimited)
            StripeChange.UnknownPrice -> {
                Logger.e(TAG) { "The server does not sell ${price.sku}" }
                loadCatalog()
                finish(Notice.Failed)
            }
            StripeChange.CardDeclined -> {
                retryable = price
                finish(Notice.CardDeclined)
            }
            StripeChange.Unavailable -> {
                retryable = price
                finish(Notice.ChangeUnavailable)
            }
            StripeChange.Failed -> finish(Notice.Failed)
        }
    }

    private suspend fun refreshPlan(refresh: Boolean = true) {
        val plan = purchases.currentPlan(refresh)
        val knownCustomer = purchases.hasStripeCustomer()
        val coverage = purchases.coveredElsewhere()
        _viewState.update {
            it.copy(
                plan = plan,
                loadingPlan = false,
                knownCustomer = knownCustomer,
                coveredElsewhere = coverage.covered,
                coveredBy = coverage.source,
            )
        }
    }

    private suspend fun loadCatalog() {
        var attempts = 0
        while (true) {
            val prices = purchases.prices()
            attempts++
            if (prices == null &&
                attempts < CATALOG_ATTEMPTS &&
                _viewState.value.catalog.isEmpty
            ) {
                delay(CATALOG_RETRY_MILLIS)
                continue
            }
            _viewState.update {
                it.copy(
                    catalog = prices?.let(BillingCatalog::from) ?: it.catalog,
                    loadingPrices = false,
                )
            }
            if (_viewState.value.catalog.isEmpty) {
                logCheckout(
                    BillingFlow.QUERY_SKUS_FAILED,
                    sku = null,
                    result = if (prices == null) {
                        AnalyticsEvents.Checkout.UNAVAILABLE
                    } else {
                        AnalyticsEvents.Checkout.EMPTY_CATALOG
                    },
                )
            }
            return
        }
    }

    private fun lingerIfLeft() {
        val left = checkout?.takeIf { quiet } ?: return
        abandon(left, AnalyticsEvents.Checkout.BROWSER_CLOSED, interval = POLL_INTERVAL_MILLIS)
    }

    private fun abandon(
        attempt: Attempt,
        reason: String,
        interval: Long = ABANDONED_POLL_MILLIS,
    ) {
        if (attempt.outcome == Outcome.PAID || attempt.watched) return
        if (attempt.inFlight > 0) {
            attempt.endWhenReturned = { abandon(attempt, reason, interval) }
            return
        }
        if (attempt.code.isEmpty()) {
            end(attempt, reason)
            return
        }
        attempt.watched = true
        background.launch(Dispatchers.Main) {
            val outcome = when (val first = collect(attempt)) {
                is StripeCollection.Collected -> first.outcome
                StripeCollection.Pending,
                is StripeCollection.RateLimited,
                StripeCollection.Unreachable -> {
                    end(attempt, reason)
                    withTimeoutOrNull(attempt.expiresAt.lifetime) {
                        awaitCollection(attempt, interval)
                    }
                }
                StripeCollection.Gone -> {
                    end(attempt, AnalyticsEvents.Checkout.GONE)
                    null
                }
                StripeCollection.SubscriptionInvalid -> {
                    end(attempt, AnalyticsEvents.Checkout.SUBSCRIPTION_INVALID)
                    null
                }
            }
            if (outcome != null && viewModelScope.isActive) collectedLater(attempt, outcome)
        }
    }

    private suspend fun collectedLater(left: Attempt, outcome: StripeRestoreOutcome) {
        checkout
            ?.takeIf { it !== left }
            ?.let { abandon(it, AnalyticsEvents.Checkout.SUPERSEDED) }
        pollJob?.cancel()
        refreshPlan()
        finish(outcome.notice)
    }

    private suspend fun awaitCollection(attempt: Attempt, interval: Long): StripeRestoreOutcome? {
        var wait = interval
        var unanswered = 0
        while (true) {
            delay(wait)
            when (val result = collect(attempt)) {
                is StripeCollection.Collected -> return result.outcome
                StripeCollection.Gone,
                StripeCollection.SubscriptionInvalid -> return null
                StripeCollection.Pending -> {
                    unanswered = 0
                    wait = (wait * 2).coerceAtMost(MAX_ABANDONED_POLL_MILLIS)
                }
                StripeCollection.Unreachable -> {
                    unanswered++
                    if (unanswered >= MAX_UNANSWERED_POLLS) return null
                    wait = unansweredBackoff(unanswered)
                }
                is StripeCollection.RateLimited -> {
                    unanswered = 0
                    wait = result.backoff
                }
            }
        }
    }

    private val Long.lifetime: Long
        get() = (this - currentTimeMillis())
            .takeIf { this > 0 && it > 0 }
            ?: DEFAULT_CODE_LIFETIME_MILLIS

    private fun poll(
        attempt: Attempt,
        firstWait: Long = POLL_INTERVAL_MILLIS,
        onUnsettled: () -> Unit = {},
    ) {
        val window = attempt.expiresAt.lifetime
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val end = withTimeoutOrNull(window) { pollUntilSettled(attempt, firstWait, onUnsettled) }
            when {
                end == null -> {
                    end(attempt, AnalyticsEvents.Checkout.EXPIRED)
                    finish(if (quiet) null else Notice.Expired)
                }
                else -> {
                    currentCoroutineContext().ensureActive()
                    refreshPlan()
                    finish(end.notice.takeIf { end.collected || !quiet })
                }
            }
        }
    }

    private suspend fun pollUntilSettled(
        attempt: Attempt,
        firstWait: Long,
        onUnsettled: () -> Unit,
    ): PollEnd {
        var wait = firstWait
        var unanswered = 0
        while (true) {
            delay(wait)
            when (val result = collect(attempt)) {
                StripeCollection.Pending -> {
                    onUnsettled()
                    unanswered = 0
                    wait = POLL_INTERVAL_MILLIS
                }
                StripeCollection.Unreachable -> {
                    onUnsettled()
                    unanswered++
                    if (unanswered >= MAX_UNANSWERED_POLLS) {
                        Logger.e(TAG) { "Giving up on the checkout: the server stopped answering" }
                        abandon(attempt, AnalyticsEvents.Checkout.UNREACHABLE)
                        return PollEnd(Notice.Unavailable)
                    }
                    wait = unansweredBackoff(unanswered)
                }
                is StripeCollection.RateLimited -> {
                    onUnsettled()
                    unanswered = 0
                    wait = result.backoff
                }
                is StripeCollection.Collected ->
                    return PollEnd(result.outcome.notice, collected = true)
                StripeCollection.Gone -> {
                    end(attempt, AnalyticsEvents.Checkout.GONE)
                    return PollEnd(Notice.Expired)
                }
                StripeCollection.SubscriptionInvalid -> {
                    end(attempt, AnalyticsEvents.Checkout.SUBSCRIPTION_INVALID)
                    return PollEnd(Notice.SubscriptionInvalid)
                }
            }
        }
    }

    private class PollEnd(val notice: Notice?, val collected: Boolean = false)

    private fun unansweredBackoff(unanswered: Int): Long =
        (POLL_INTERVAL_MILLIS shl (unanswered - 1))
            .coerceIn(POLL_INTERVAL_MILLIS, MAX_BACKOFF_MILLIS)

    private val StripeCollection.RateLimited.backoff: Long
        get() = (retryAfterMillis ?: RATE_LIMITED_BACKOFF_MILLIS)
            .coerceIn(POLL_INTERVAL_MILLIS, MAX_BACKOFF_MILLIS)

    private fun finish(notice: Notice?) {
        pollJob = null
        checkout = null
        quiet = false
        _viewState.update { it.copy(busySku = null, checkoutUrl = null, notice = notice) }
    }

    private fun logCheckout(
        state: String,
        sku: String?,
        result: String? = null,
        late: Boolean = false,
    ) = reporting.logBillingFlow(
        BillingProvider.STRIPE,
        state,
        sku = sku,
        result = result,
        *if (late) arrayOf(AnalyticsEvents.PARAM_LATE to true) else emptyArray(),
    )

    private fun startAttempt(price: BillingOption): Attempt =
        Attempt(price.sku).also {
            attempts.add(it)
            markLaunched(it)
        }

    private fun markLaunched(attempt: Attempt) {
        attempt.launched = true
        logCheckout(BillingFlow.LAUNCHED, attempt.sku)
    }

    private fun terminal(
        attempt: Attempt,
        state: String,
        result: String,
        log: (late: Boolean) -> Unit = { logCheckout(state, attempt.sku, result, it) },
    ) {
        val paid = state == BillingFlow.PURCHASED
        val late = attempt.outcome == Outcome.FAILED
        when (attempt.outcome) {
            Outcome.PAID -> return
            Outcome.FAILED -> if (!paid) return
            Outcome.OPEN -> Unit
        }
        attempt.outcome = if (paid) Outcome.PAID else Outcome.FAILED
        attempts.remove(attempt)
        log(late)
    }

    private fun fail(attempt: Attempt, result: String) = terminal(
        attempt,
        if (attempt.launched) BillingFlow.PURCHASE_UPDATED_FAILED else BillingFlow.PURCHASE_FAILED,
        result,
    )

    private fun end(attempt: Attempt, result: String) {
        if (attempt.launched) terminal(attempt, BillingFlow.PURCHASE_UPDATED_FAILED, result)
    }

    private fun collected(attempt: Attempt, outcome: StripeRestoreOutcome) =
        terminal(attempt, BillingFlow.PURCHASED, outcome.analyticsResult)

    private fun restored(attempt: Attempt, outcome: StripeRestoreOutcome) =
        terminal(attempt, BillingFlow.PURCHASED, outcome.analyticsResult) {
            reporting.logStripeRestore(outcome)
        }

    private suspend fun collect(attempt: Attempt): StripeCollection = attempt.guarded {
        purchases.collectCheckout(attempt.code).also {
            if (it is StripeCollection.Collected) collected(attempt, it.outcome)
        }
    }

    private suspend fun <T> Attempt.guarded(block: suspend () -> T): T =
        withContext(NonCancellable) {
            inFlight++
            try {
                block()
            } finally {
                inFlight--
                if (inFlight == 0) {
                    endWhenReturned?.let {
                        endWhenReturned = null
                        it()
                    }
                }
            }
        }.also { currentCoroutineContext().ensureActive() }

    private val StripeCheckout.errorReason: String?
        get() = when (this) {
            is StripeCheckout.Started,
            is StripeCheckout.Collected,
            is StripeCheckout.SubscriptionExists -> null
            is StripeCheckout.AccountInUse -> AnalyticsEvents.Checkout.ACCOUNT_IN_USE
            StripeCheckout.AccountLinkConflict -> AnalyticsEvents.Checkout.ACCOUNT_LINK_CONFLICT
            StripeCheckout.RestoreRequired -> AnalyticsEvents.Checkout.RESTORE_REQUIRED
            StripeCheckout.RateLimited -> AnalyticsEvents.Checkout.RATE_LIMITED
            StripeCheckout.Unavailable -> AnalyticsEvents.Checkout.UNAVAILABLE
            StripeCheckout.UnknownPrice -> AnalyticsEvents.Checkout.UNKNOWN_PRICE
            StripeCheckout.Failed -> AnalyticsEvents.Checkout.FAILED
        }

    private val StripeChange.resultName: String
        get() = when (this) {
            is StripeChange.Changed ->
                if (resumed) AnalyticsEvents.Checkout.RESUMED else AnalyticsEvents.Checkout.CHANGED
            StripeChange.NotLinked -> AnalyticsEvents.Checkout.NOT_LINKED
            StripeChange.Cancelled -> AnalyticsEvents.Checkout.CANCELLED
            StripeChange.PurchaseGone -> AnalyticsEvents.Checkout.PURCHASE_GONE
            StripeChange.NoProofOfPurchase -> AnalyticsEvents.Checkout.NO_PROOF_OF_PURCHASE
            StripeChange.CardDeclined -> AnalyticsEvents.Checkout.CARD_DECLINED
            StripeChange.RateLimited -> AnalyticsEvents.Checkout.RATE_LIMITED
            StripeChange.UnknownPrice -> AnalyticsEvents.Checkout.UNKNOWN_PRICE
            StripeChange.Unavailable -> AnalyticsEvents.Checkout.UNAVAILABLE
            StripeChange.Failed -> AnalyticsEvents.Checkout.FAILED
        }

    private val StripeRestoreOutcome.notice: Notice?
        get() = when (this) {
            StripeRestoreOutcome.Restored -> null
            StripeRestoreOutcome.Ended -> Notice.SubscriptionCancelled
            StripeRestoreOutcome.NeedsUpgrade -> Notice.NeedsUpgrade
            is StripeRestoreOutcome.AccountInUse -> Notice.AccountInUse(existingAccount)
            is StripeRestoreOutcome.StorageFailed ->
                if (encryptionFailed) Notice.EncryptionFailed else Notice.Failed
        }

    companion object {
        const val POLL_INTERVAL_MILLIS = 3_000L
        const val ABANDONED_POLL_MILLIS = 30_000L
        const val RATE_LIMITED_BACKOFF_MILLIS = 30_000L
        const val MAX_BACKOFF_MILLIS = 60_000L
        const val MAX_ABANDONED_POLL_MILLIS = 5 * 60_000L
        const val MAX_UNANSWERED_POLLS = 8
        const val CATALOG_ATTEMPTS = 2
        const val CATALOG_RETRY_MILLIS = 1_000L
        private const val DEFAULT_CODE_LIFETIME_MILLIS = 60 * 60 * 1000L
    }
}
