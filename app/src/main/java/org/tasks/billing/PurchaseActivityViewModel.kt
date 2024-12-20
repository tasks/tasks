package org.tasks.billing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PurchaseActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inventory: Inventory,
    private val billingClient: BillingClient,
    private val localBroadcastManager: LocalBroadcastManager,
    firebase: Firebase,
) : ViewModel() {

    data class ViewState(
        val nameYourPrice: Boolean,
        val isGithub: Boolean,
        val price: Float = -1f,
        val subscription: Purchase? = null,
        val error: String? = null,
    )

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val subscription = inventory.subscription.value
            _viewState.update { state ->
                state.copy(
                    subscription = subscription,
                    price = state.price.takeIf { it > 0 }
                        ?: subscription?.subscriptionPrice?.coerceAtMost(25)?.toFloat()
                        ?: 10f
                )
            }
        }
    }

    fun setPrice(price: Float) {
        _viewState.update { it.copy(price = price) }
    }

    private val _viewState = MutableStateFlow(
        ViewState(
            nameYourPrice = savedStateHandle.get<Boolean>(EXTRA_NAME_YOUR_PRICE) ?: firebase.nameYourPrice,
            isGithub = savedStateHandle.get<Boolean>(EXTRA_GITHUB) ?: false,
        )
    )
    val viewState: StateFlow<ViewState> = _viewState

    init {
        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)

        viewModelScope.launch {
            try {
                billingClient.queryPurchases(throwError = true)
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(error = e.message)
                }
            }
        }

        firebase.logEvent(R.string.event_showed_purchase_dialog)
    }

    override fun onCleared() {
        super.onCleared()
        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    fun purchase(activity: Activity, price: Int, monthly: Boolean) = viewModelScope.launch {
        val newSku = String.format(Locale.US, "%s_%02d", if (monthly) "monthly" else "annual", price)
        try {
            billingClient.initiatePurchaseFlow(
                activity,
                newSku,
                BillingClientImpl.TYPE_SUBS,
                _viewState.value.subscription?.takeIf { it.sku != newSku },
            )
        } catch (e: Exception) {
            _viewState.update {
                it.copy(error = e.message)
            }
        }
    }

    fun setNameYourPrice(nameYourPrice: Boolean) {
        _viewState.update {  it.copy(nameYourPrice = nameYourPrice) }
    }

    companion object {
        const val EXTRA_GITHUB = "extra_github"
        const val EXTRA_NAME_YOUR_PRICE = "extra_name_your_price"
    }
}
