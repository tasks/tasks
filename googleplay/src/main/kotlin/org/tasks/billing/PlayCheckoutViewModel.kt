package org.tasks.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting

private const val TAG = "PlayCheckout"

open class PlayCheckoutViewModel(
    private val billing: PlayBilling,
    private val purchases: PlayPurchases,
    private val reporting: Reporting,
    coveredBy: Flow<UnlockSource?> = flowOf(null),
) : ViewModel() {
    data class ViewState(
        val catalog: BillingCatalog = BillingCatalog(),
        val loadingPrices: Boolean = true,
        val busySku: String? = null,
        val currentSku: String? = null,
        val coveredBy: UnlockSource? = null,
        val error: Error? = null,
    ) {
        val coveredElsewhere: Boolean get() = coveredBy != null
    }

    sealed interface Error {
        data object Unavailable : Error

        data class Failed(val message: String?) : Error
    }

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch { refreshPurchases() }
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch {
            purchases.subscription.collect { purchase ->
                _viewState.update { it.copy(currentSku = purchase?.sku) }
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
        _viewState.update { it.copy(busySku = price.sku) }
        viewModelScope.launch {
            val current = purchases.state.value.subscription
            try {
                billing.launchPurchaseFlow(
                    activity = activity,
                    sku = price.sku,
                    oldPurchaseToken = current?.purchaseToken?.takeIf { current.sku != price.sku },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, TAG) { "Failed to launch purchase flow for ${price.sku}" }
                reporting.logEvent(
                    AnalyticsEvents.CHECKOUT_ERROR,
                    AnalyticsEvents.PARAM_SKU to price.sku,
                    AnalyticsEvents.PARAM_RESULT to (e.message ?: AnalyticsEvents.Checkout.FAILED),
                )
                _viewState.update { it.copy(error = e.toError()) }
            } finally {
                _viewState.update { it.copy(busySku = null) }
            }
        }
    }

    fun retry() {
        _viewState.update { it.copy(error = null) }
        viewModelScope.launch { loadCatalog() }
    }

    fun dismissError() {
        _viewState.update { it.copy(error = null) }
    }

    private suspend fun refreshPurchases() {
        try {
            billing.queryPurchases()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e, TAG) { "Failed to query purchases" }
            _viewState.update { it.copy(error = e.toError()) }
        }
    }

    private suspend fun loadCatalog() {
        _viewState.update { it.copy(loadingPrices = true) }
        try {
            val products = billing.products(PlayCatalog.ALL_SKUS)
            Logger.d(TAG) { "Loaded ${products.size}/${PlayCatalog.ALL_SKUS.size} products" }
            _viewState.update {
                it.copy(catalog = PlayCatalog.toCatalog(products), loadingPrices = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, TAG) { "Failed to load catalog" }
            reporting.logEvent(
                AnalyticsEvents.BILLING_FLOW_RESULT,
                AnalyticsEvents.PARAM_RESULT to (e.message ?: "unknown").take(100),
                AnalyticsEvents.PARAM_STATE to STATE_QUERY_SKUS_FAILED,
            )
            _viewState.update { it.copy(loadingPrices = false, error = e.toError()) }
        }
    }

    private fun Exception.toError(): Error = when (this) {
        is PlayBillingUnavailableException -> Error.Unavailable
        else -> Error.Failed(message)
    }

    companion object {
        private const val STATE_QUERY_SKUS_FAILED = "QUERY_SKUS_FAILED"
    }
}
