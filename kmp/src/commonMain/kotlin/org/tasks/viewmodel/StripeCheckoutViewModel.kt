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
import org.tasks.billing.StripeCatalog
import org.tasks.billing.StripeChange
import org.tasks.billing.StripeCheckout
import org.tasks.billing.StripeCheckouts
import org.tasks.billing.StripeCollection
import org.tasks.billing.StripePrice
import org.tasks.billing.StripePlan
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private const val TAG = "StripeCheckout"

open class StripeCheckoutViewModel(
    private val purchases: StripeCheckouts,
) : ViewModel() {

    data class ViewState(
        val catalog: StripeCatalog = StripeCatalog(),
        val loadingPrices: Boolean = true,
        val plan: StripePlan? = null,
        val busySku: String? = null,
        val checkoutUrl: String? = null,
        val confirmChange: PendingChange? = null,
        val planChanged: Boolean = false,
        val notice: Notice? = null,
    ) {
        val busy: Boolean get() = busySku != null
    }

    data class PendingChange(
        val price: StripePrice,
        val trialEnd: Long? = null,
    )

    sealed interface Notice {
        data object NeedsUpgrade : Notice

        data object PurchaseGone : Notice

        data class AccountInUse(val existingAccount: String) : Notice

        data object Expired : Notice

        data object SubscriptionInvalid : Notice

        data object SubscriptionCancelled : Notice

        data object StorageFailed : Notice

        data object RateLimited : Notice

        data object Unavailable : Notice

        data object Failed : Notice
    }

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch { refreshPlan() }
    }

    fun select(price: StripePrice) {
        if (_viewState.value.busy) return
        val plan = _viewState.value.plan
        when {
            plan == null -> purchase(price)
            plan.sku == price.sku -> return
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

    fun dismissChange() = _viewState.update { it.copy(confirmChange = null) }

    fun cancel() {
        pollJob?.cancel()
        pollJob = null
        _viewState.update { it.copy(busySku = null, checkoutUrl = null) }
    }

    fun dismissNotice() = _viewState.update { it.copy(notice = null) }

    fun planChangeHandled() = _viewState.update { it.copy(planChanged = false) }

    private fun purchase(price: StripePrice) {
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch {
            when (val checkout = purchases.createCheckout(price)) {
                is StripeCheckout.Started -> {
                    _viewState.update { it.copy(checkoutUrl = checkout.url) }
                    poll(checkout.code, checkout.expiresAt)
                }
                is StripeCheckout.SubscriptionExists -> {
                    Logger.w(TAG) { "Checkout refused: this account already pays for ${checkout.product}" }
                    refreshPlan()
                    changeNow(price)
                }
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

    private fun change(price: StripePrice) {
        _viewState.update { it.copy(busySku = price.sku, notice = null) }
        viewModelScope.launch { changeNow(price) }
    }

    private suspend fun changeNow(price: StripePrice) {
        when (val result = purchases.changePlan(price)) {
            is StripeChange.Changed -> {
                refreshPlan()
                finish(null)
                _viewState.update { it.copy(planChanged = true) }
            }
            StripeChange.NotLinked -> {
                _viewState.update { it.copy(plan = null) }
                purchase(price)
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
            StripeChange.Unavailable -> finish(Notice.Unavailable)
            StripeChange.Failed -> finish(Notice.Failed)
        }
    }

    private suspend fun refreshPlan() {
        val plan = purchases.currentPlan()
        _viewState.update { it.copy(plan = plan) }
    }

    private suspend fun loadCatalog() {
        val prices = purchases.prices()
        _viewState.update {
            it.copy(
                catalog = prices?.let(StripeCatalog::from) ?: it.catalog,
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
        while (true) {
            delay(wait)
            when (val result = purchases.collectCheckout(code)) {
                StripeCollection.Pending -> wait = POLL_INTERVAL_MILLIS
                is StripeCollection.RateLimited -> wait = result.backoff
                is StripeCollection.Collected -> return PollEnd(result.outcome.notice)
                StripeCollection.Gone -> return PollEnd(Notice.Expired)
                StripeCollection.SubscriptionInvalid -> return PollEnd(Notice.SubscriptionInvalid)
            }
        }
    }

    private class PollEnd(val notice: Notice?)

    private val StripeCollection.RateLimited.backoff: Long
        get() = (retryAfterMillis ?: RATE_LIMITED_BACKOFF_MILLIS)
            .coerceIn(POLL_INTERVAL_MILLIS, MAX_BACKOFF_MILLIS)

    private fun finish(notice: Notice?) {
        pollJob = null
        _viewState.update { it.copy(busySku = null, checkoutUrl = null, notice = notice) }
    }

    private val StripeRestoreOutcome.notice: Notice?
        get() = when (this) {
            StripeRestoreOutcome.Restored -> null
            StripeRestoreOutcome.NeedsUpgrade -> Notice.NeedsUpgrade
            is StripeRestoreOutcome.AccountInUse -> Notice.AccountInUse(existingAccount)
            StripeRestoreOutcome.StorageFailed -> Notice.StorageFailed
        }

    companion object {
        const val POLL_INTERVAL_MILLIS = 3_000L
        const val RATE_LIMITED_BACKOFF_MILLIS = 30_000L
        const val MAX_BACKOFF_MILLIS = 60_000L
        private const val DEFAULT_CODE_LIFETIME_MILLIS = 60 * 60 * 1000L
    }
}
