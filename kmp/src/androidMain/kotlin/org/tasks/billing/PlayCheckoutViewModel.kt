package org.tasks.billing

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.analytics.AnalyticsEvents
import org.tasks.TasksUrls
import org.tasks.analytics.Reporting
import org.tasks.analytics.logBillingFlow

private const val TAG = "PlayCheckout"

open class PlayCheckoutViewModel(
    private val billing: PlayBilling,
    private val purchases: PlayPurchases,
    private val reporting: Reporting,
    coveredBy: Flow<UnlockSource?> = flowOf(null),
    private val openSubscriptionCenter: (Activity, String) -> Unit = ::openPlaySubscriptionCenter,
) : ViewModel() {
    data class ViewState(
        val catalog: BillingCatalog = BillingCatalog(),
        val loadingPrices: Boolean = true,
        val busySku: String? = null,
        val currentSku: String? = null,
        val currentCanceled: Boolean = false,
        val loadingPlan: Boolean = true,
        val coveredBy: UnlockSource? = null,
        val error: Error? = null,
        val purchased: Boolean = false,
    ) {
        val coveredElsewhere: Boolean get() = coveredBy != null
    }

    sealed interface Error {
        val price: BillingOption

        data class Unavailable(override val price: BillingOption) : Error

        data class Failed(val message: String?, override val price: BillingOption) : Error
    }

    private data class Pending(val sku: String, val oldPurchaseToken: String?)

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()
    private var pending: Pending? = null

    init {
        viewModelScope.launch { refreshPurchases() }
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch {
            purchases.load()
            purchases.state.collect { state ->
                val purchase = state.subscription
                val completed = pending?.let {
                    val bought = state.purchases[it.sku]
                    bought != null &&
                        bought.purchaseToken != it.oldPurchaseToken
                } == true
                if (completed) {
                    pending = null
                }
                _viewState.update {
                    it.copy(
                        currentSku = purchase?.sku,
                        currentCanceled = purchase?.isCanceled == true,
                        loadingPlan = false,
                        purchased = it.purchased || completed,
                    )
                }
            }
        }
        viewModelScope.launch {
            coveredBy.collect { source ->
                _viewState.update { it.copy(coveredBy = source) }
            }
        }
    }

    fun select(price: BillingOption, activity: Activity) {
        if (_viewState.value.busySku != null) {
            return
        }
        val current = purchases.state.value.subscription
        if (current?.sku == price.sku && current.isCanceled) {
            reporting.logBillingFlow(
                BillingProvider.GOOGLE_PLAY,
                AnalyticsEvents.BillingFlow.SUBSCRIPTION_CENTER,
                sku = price.sku,
            )
            openSubscriptionCenter(activity, price.sku)
            return
        }
        _viewState.update { it.copy(busySku = price.sku, error = null) }
        viewModelScope.launch {
            try {
                pending = Pending(price.sku, current?.purchaseToken)
                billing.launchPurchaseFlow(
                    activity = activity,
                    sku = price.sku,
                    oldPurchaseToken = current?.purchaseToken?.takeIf { current.sku != price.sku },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pending = null
                Logger.e(e, TAG) { "Failed to launch purchase flow for ${price.sku}" }
                _viewState.update { it.copy(error = e.toError(price)) }
            } finally {
                _viewState.update { it.copy(busySku = null) }
            }
        }
    }

    fun dismissError() {
        _viewState.update { it.copy(error = null) }
    }

    fun retry(activity: Activity) {
        val error = _viewState.value.error ?: return
        dismissError()
        if (error !is Error.Unavailable || !sellable(error.price)) {
            return
        }
        select(error.price, activity)
    }

    fun purchaseHandled() {
        _viewState.update { it.copy(purchased = false) }
    }

    private fun sellable(price: BillingOption): Boolean =
        _viewState.value.let { price.sku != it.currentSku || it.currentCanceled }

    private suspend fun refreshPurchases() {
        try {
            billing.queryPurchases()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e, TAG) { "Failed to query purchases" }
        }
    }

    private suspend fun loadCatalog() {
        _viewState.update { it.copy(loadingPrices = true) }
        var attempts = 0
        while (true) {
            attempts++
            try {
                val catalog = PlayCatalog.toCatalog(billing.products(PlayCatalog.ALL_SKUS))
                if (catalog.isEmpty && attempts < CATALOG_ATTEMPTS) {
                    delay(CATALOG_RETRY_MILLIS)
                    continue
                }
                Logger.d(TAG) { "Loaded catalog after $attempts attempt(s)" }
                if (catalog.isEmpty) {
                    reporting.logBillingFlow(
                        BillingProvider.GOOGLE_PLAY,
                        AnalyticsEvents.BillingFlow.QUERY_SKUS_FAILED,
                        result = AnalyticsEvents.Checkout.EMPTY_CATALOG,
                    )
                }
                _viewState.update { it.copy(catalog = catalog, loadingPrices = false) }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempts < CATALOG_ATTEMPTS) {
                    Logger.w(e, TAG) { "Failed to load catalog, retrying" }
                    delay(CATALOG_RETRY_MILLIS)
                    continue
                }
                Logger.e(e, TAG) { "Failed to load catalog" }
                reporting.logBillingFlow(
                    BillingProvider.GOOGLE_PLAY,
                    AnalyticsEvents.BillingFlow.QUERY_SKUS_FAILED,
                    result = e.message ?: AnalyticsEvents.Checkout.FAILED,
                )
                _viewState.update { it.copy(loadingPrices = false) }
                return
            }
        }
    }

    private fun Exception.toError(price: BillingOption): Error = when (this) {
        is PlayBillingUnavailableException -> Error.Unavailable(price)
        else -> Error.Failed(message, price)
    }

    companion object {
        const val CATALOG_ATTEMPTS = 2
        const val CATALOG_RETRY_MILLIS = 1_000L
    }
}

private fun openPlaySubscriptionCenter(activity: Activity, sku: String) {
    val uri = "${TasksUrls.GOOGLE_PLAY_SUBSCRIPTIONS}?sku=$sku&package=${activity.packageName}".toUri()
    try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Logger.e(e, TAG) { "Failed to open the Play subscription center for $sku" }
    }
}
