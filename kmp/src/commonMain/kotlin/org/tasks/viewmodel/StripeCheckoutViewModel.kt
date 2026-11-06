package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.billing.BillingCatalog
import org.tasks.billing.StripeChange
import org.tasks.billing.StripeCheckout
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeCollection
import org.tasks.billing.BillingOption
import org.tasks.billing.StripePlan
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.UnlockSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private const val TAG = "StripeCheckout"

open class StripeCheckoutViewModel(
    private val purchases: StripeCheckouts,
    private val reporting: Reporting,
) : ViewModel() {

    data class ViewState(
        val catalog: BillingCatalog = BillingCatalog(),
        val loadingPrices: Boolean = true,
        val plan: StripePlan? = null,
        val busySku: String? = null,
        val checkoutUrl: String? = null,
        val confirmChange: PendingChange? = null,
        val planChanged: Boolean = false,
        val knownCustomer: Boolean = false,
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

    private var retryable: BillingOption? = null

    init {
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch { refreshPlan() }
    }

    fun select(price: BillingOption) {
        if (_viewState.value.busy) return
        val plan = _viewState.value.plan
        when {
            plan == null || !plan.canChange -> purchase(price)
            plan.sku == price.sku && !plan.resumable -> return
            else -> _viewState.update {
                it.copy(
                    confirmChange = PendingChange(
                        price = price,
                        trialEnd = plan.trialEnd,
                    )
                )
            }
        }
    }

    fun confirmChange() {
        val pending = _viewState.value.confirmChange ?: return
        _viewState.update { it.copy(confirmChange = null) }
        change(pending.price)
    }

    fun dismissChange() {
        _viewState.value.confirmChange
            ?.let { logPlanChange(it.price, AnalyticsEvents.Checkout.DISMISSED) }
        _viewState.update { it.copy(confirmChange = null) }
    }

    fun cancel() {
        if (_viewState.value.checkoutUrl != null) {
            logCheckout(AnalyticsEvents.CHECKOUT_ABANDONED, _viewState.value.busySku)
        }
        pollJob?.cancel()
        pollJob = null
        _viewState.update { it.copy(busySku = null, checkoutUrl = null) }
    }

    fun dismissNotice() {
        retryable = null
        _viewState.update { it.copy(notice = null) }
    }

    fun retryChange() {
        val price = retryable ?: return
        retryable = null
        _viewState.update { it.copy(notice = null) }
        change(price)
    }

    fun planChangeHandled() = _viewState.update { it.copy(planChanged = false) }

    private fun purchase(price: BillingOption) {
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch {
            val checkout = purchases.createCheckout(price)
            checkout.errorReason?.let { logCheckoutError(it, price.sku) }
            when (checkout) {
                is StripeCheckout.Started -> {
                    logCheckout(
                        AnalyticsEvents.CHECKOUT_STARTED,
                        price.sku,
                        AnalyticsEvents.PARAM_PERIOD to price.period,
                    )
                    _viewState.update { it.copy(checkoutUrl = checkout.url) }
                    poll(checkout.code, checkout.expiresAt)
                }
                is StripeCheckout.AccountInUse ->
                    finish(Notice.AccountInUse(checkout.existingAccount))
                is StripeCheckout.Collected -> {
                    logCollected(checkout.outcome, price.sku)
                    refreshPlan()
                    finish(checkout.outcome.notice)
                }
                is StripeCheckout.SubscriptionExists -> {
                    Logger.w(TAG) { "Checkout refused: this account already pays for ${checkout.product}" }
                    refreshPlan()
                    changeNow(price, sellInstead = false)
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

    private fun change(price: BillingOption) {
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch { changeNow(price) }
    }

    private suspend fun changeNow(price: BillingOption, sellInstead: Boolean = true) {
        val result = purchases.changePlan(price)
        logPlanChange(price, result.resultName)
        when (result) {
            is StripeChange.Changed -> {
                refreshPlan(refresh = false)
                finish(null)
                _viewState.update { it.copy(planChanged = true) }
            }
            StripeChange.NotLinked -> if (sellInstead) {
                _viewState.update { it.copy(plan = null) }
                purchase(price)
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
                knownCustomer = knownCustomer,
                coveredElsewhere = coverage.covered,
                coveredBy = coverage.source,
            )
        }
    }

    private suspend fun loadCatalog() {
        val prices = purchases.prices()
        _viewState.update {
            it.copy(
                catalog = prices?.let(BillingCatalog::from) ?: it.catalog,
                loadingPrices = false,
            )
        }
    }

    private fun poll(code: String, expiresAt: Long) {
        val window = (expiresAt - currentTimeMillis())
            .takeIf { expiresAt > 0 && it > 0 }
            ?: DEFAULT_CODE_LIFETIME_MILLIS
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val end = withTimeoutOrNull(window) { pollUntilSettled(code) }
            if (end == null) {
                logCheckoutError(AnalyticsEvents.Checkout.EXPIRED)
                finish(Notice.Expired)
            } else {
                currentCoroutineContext().ensureActive()
                refreshPlan()
                finish(end.notice)
            }
        }
    }

    private suspend fun pollUntilSettled(code: String): PollEnd {
        var wait = POLL_INTERVAL_MILLIS
        var unanswered = 0
        while (true) {
            delay(wait)
            when (val result = purchases.collectCheckout(code)) {
                StripeCollection.Pending -> {
                    unanswered = 0
                    wait = POLL_INTERVAL_MILLIS
                }
                StripeCollection.Unreachable -> {
                    unanswered++
                    if (unanswered >= MAX_UNANSWERED_POLLS) {
                        Logger.e(TAG) { "Giving up on the checkout: the server stopped answering" }
                        logCheckoutError(AnalyticsEvents.Checkout.UNREACHABLE)
                        return PollEnd(Notice.Unavailable)
                    }
                    wait = unansweredBackoff(unanswered)
                }
                is StripeCollection.RateLimited -> {
                    unanswered = 0
                    wait = result.backoff
                }
                is StripeCollection.Collected -> {
                    logCollected(result.outcome)
                    return PollEnd(result.outcome.notice)
                }
                StripeCollection.Gone -> {
                    logCheckoutError(AnalyticsEvents.Checkout.GONE)
                    return PollEnd(Notice.Expired)
                }
                StripeCollection.SubscriptionInvalid -> {
                    logCheckoutError(AnalyticsEvents.Checkout.SUBSCRIPTION_INVALID)
                    return PollEnd(Notice.SubscriptionInvalid)
                }
            }
        }
    }

    private class PollEnd(val notice: Notice?)

    private fun unansweredBackoff(unanswered: Int): Long =
        (POLL_INTERVAL_MILLIS shl (unanswered - 1))
            .coerceIn(POLL_INTERVAL_MILLIS, MAX_BACKOFF_MILLIS)

    private val StripeCollection.RateLimited.backoff: Long
        get() = (retryAfterMillis ?: RATE_LIMITED_BACKOFF_MILLIS)
            .coerceIn(POLL_INTERVAL_MILLIS, MAX_BACKOFF_MILLIS)

    private fun finish(notice: Notice?) {
        pollJob = null
        _viewState.update { it.copy(busySku = null, checkoutUrl = null, notice = notice) }
    }

    private fun logCheckout(event: String, sku: String?, vararg params: Pair<String, Any>) =
        reporting.logEvent(event, AnalyticsEvents.PARAM_SKU to sku.orEmpty(), *params)

    private fun logCheckoutError(reason: String, sku: String? = _viewState.value.busySku) =
        logCheckout(AnalyticsEvents.CHECKOUT_ERROR, sku, AnalyticsEvents.PARAM_REASON to reason)

    private fun logCollected(
        outcome: StripeRestoreOutcome,
        sku: String? = _viewState.value.busySku,
    ) = logCheckout(
        AnalyticsEvents.CHECKOUT_COLLECTED,
        sku,
        AnalyticsEvents.PARAM_RESULT to outcome.resultName,
    )

    private fun logPlanChange(price: BillingOption, result: String) = logCheckout(
        AnalyticsEvents.PLAN_CHANGE,
        price.sku,
        AnalyticsEvents.PARAM_PERIOD to price.period,
        AnalyticsEvents.PARAM_RESULT to result,
    )

    private val BillingOption.period: String
        get() = if (isMonthly) AnalyticsEvents.PERIOD_MONTHLY else AnalyticsEvents.PERIOD_ANNUAL

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

    private val StripeRestoreOutcome.resultName: String
        get() = when (this) {
            StripeRestoreOutcome.Restored -> AnalyticsEvents.Checkout.RESTORED
            StripeRestoreOutcome.Ended -> AnalyticsEvents.Checkout.ENDED
            StripeRestoreOutcome.NeedsUpgrade -> AnalyticsEvents.Checkout.NEEDS_UPGRADE
            is StripeRestoreOutcome.AccountInUse -> AnalyticsEvents.Checkout.ACCOUNT_IN_USE
            is StripeRestoreOutcome.StorageFailed ->
                if (encryptionFailed) {
                    AnalyticsEvents.Checkout.ENCRYPTION_FAILED
                } else {
                    AnalyticsEvents.Checkout.STORAGE_FAILED
                }
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
        const val RATE_LIMITED_BACKOFF_MILLIS = 30_000L
        const val MAX_BACKOFF_MILLIS = 60_000L
        const val MAX_UNANSWERED_POLLS = 8
        private const val DEFAULT_CODE_LIFETIME_MILLIS = 60 * 60 * 1000L
    }
}
