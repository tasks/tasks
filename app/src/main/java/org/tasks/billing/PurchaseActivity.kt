package org.tasks.billing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.compose.PurchaseText.PurchaseText
import org.tasks.extensions.Context.toast
import org.tasks.preferences.Preferences
import org.tasks.themes.Theme
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseActivity : AppCompatActivity(), OnPurchasesUpdated {
    @Inject lateinit var theme: Theme
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var firebase: Firebase

    private var currentSubscription: Purchase? = null
    private val purchaseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setup()
        }
    }
    private val nameYourPrice = mutableStateOf(false)
    private val sliderPosition = mutableStateOf(-1f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val github = intent?.extras?.getBoolean(EXTRA_GITHUB) ?: false

        theme.applyToContext(this)

        savedInstanceState?.let {
            nameYourPrice.value = it.getBoolean(EXTRA_NAME_YOUR_PRICE)
            sliderPosition.value = it.getFloat(EXTRA_PRICE)
        }

        setContent {
            MdcTheme {
                Dialog(onDismissRequest = { finish() }) {
                    PurchaseText(
                        nameYourPrice = nameYourPrice,
                        sliderPosition = sliderPosition,
                        github = github,
                        solidButton = firebase.moreOptionsSolid,
                        badge = firebase.moreOptionsBadge,
                        onDisplayed = { firebase.logEvent(R.string.event_showed_purchase_dialog) },
                        subscribe = this::purchase,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)
        lifecycleScope.launch {
            try {
                billingClient.queryPurchases(throwError = true)
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private fun setup() {
        currentSubscription = inventory.subscription.value
        if (sliderPosition.value < 0) {
            sliderPosition.value =
                currentSubscription
                    ?.subscriptionPrice
                    ?.coerceAtMost(25)
                    ?.toFloat() ?: 10f
        }
    }

    private fun purchase(price: Int, monthly: Boolean) = lifecycleScope.launch {
        val newSku = String.format(Locale.US, "%s_%02d", if (monthly) "monthly" else "annual", price)
        try {
            billingClient.initiatePurchaseFlow(
                this@PurchaseActivity,
                newSku,
                BillingClientImpl.TYPE_SUBS,
                currentSubscription?.takeIf { it.sku != newSku }
            )
        } catch (e: Exception) {
            this@PurchaseActivity.toast(e.message)
        }
    }

    override fun onPurchasesUpdated(success: Boolean) {
        if (success) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(EXTRA_PRICE, sliderPosition.value)
        outState.putBoolean(EXTRA_NAME_YOUR_PRICE, nameYourPrice.value)
    }

    companion object {
        const val EXTRA_GITHUB = "extra_github"
        private const val EXTRA_PRICE = "extra_price"
        private const val EXTRA_NAME_YOUR_PRICE = "extra_name_your_price"
    }
}