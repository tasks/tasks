package org.tasks.billing

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Inventory @Inject constructor(
        private val preferences: Preferences,
        private val signatureVerifier: SignatureVerifier,
        private val localBroadcastManager: LocalBroadcastManager
) {
    private val purchases: MutableMap<String, Purchase> = HashMap()

    fun clear() {
        Timber.d("clear()")
        purchases.clear()
    }

    fun add(purchases: Iterable<Purchase>) {
        verifyAndAdd(purchases)
        preferences.setPurchases(this.purchases.values)
        localBroadcastManager.broadcastPurchasesUpdated()
    }

    private fun verifyAndAdd(items: Iterable<Purchase>) {
        for (purchase in items) {
            if (signatureVerifier.verifySignature(purchase)) {
                Timber.d("add(%s)", purchase)
                this.purchases[purchase.sku] = purchase
            }
        }
        hasPro = purchases.values.any { it.isProSubscription } || purchases.containsKey(SKU_VIP)
    }

    fun purchasedTasker() = hasPro || purchases.containsKey(SKU_TASKER)

    fun purchasedThemes() = hasPro || purchases.containsKey(SKU_THEMES)

    var hasPro = false
        get() {
            return BuildConfig.FLAVOR == "generic"
                    || (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false))
                    || field
        }
        private set

    fun purchased(sku: String) = purchases.containsKey(sku)

    fun getPurchase(sku: String) = purchases[sku]

    val subscription: Purchase?
        get() = purchases
                .values
                .filter { it.isProSubscription }
                .sortedWith{ l, r ->
                    r.isMonthly.compareTo(l.isMonthly)
                            .takeIf { it != 0 }?.let { return@sortedWith it }
                    l.isCanceled.compareTo(r.isCanceled)
                            .takeIf { it != 0 }?.let { return@sortedWith it }
                    return@sortedWith r.subscriptionPrice!!.compareTo(l.subscriptionPrice!!)
                }
                .firstOrNull()

    val hasTasksSubscription: Boolean
        get() = subscription?.isTasksSubscription ?: false

    fun unsubscribe(context: Context): Boolean {
        subscription?.let {
            context.startActivity(
                    Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.manage_subscription_url, it.sku)))
            )
        }
        return false
    }

    companion object {
        private const val SKU_VIP = "vip"
        const val SKU_TASKER = "tasker"
        const val SKU_THEMES = "themes"
    }

    init {
        verifyAndAdd(preferences.purchases)
    }
}
