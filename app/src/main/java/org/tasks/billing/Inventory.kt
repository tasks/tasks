package org.tasks.billing

import android.content.Context
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.dao.CaldavDao
import org.tasks.data.isTasksSubscription
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Inventory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val signatureVerifier: SignatureVerifier,
        private val localBroadcastManager: LocalBroadcastManager,
        private val caldavDao: CaldavDao
) {
    val purchases: MutableMap<String, Purchase> = HashMap()
    val subscription = MutableLiveData<Purchase?>()

    var hasTasksAccount = false
        private set

    fun clear() {
        Timber.d("clear()")
        purchases.clear()
        subscription.value = null
    }

    fun add(items: Iterable<Purchase>) {
        verifyAndAdd(items)
        preferences.setPurchases(purchases.values)
        localBroadcastManager.broadcastPurchasesUpdated()
    }

    private fun verifyAndAdd(items: Iterable<Purchase>) {
        for (purchase in items) {
            if (signatureVerifier.verifySignature(purchase)) {
                Timber.d("add(%s)", purchase)
                purchases[purchase.sku] = purchase
            }
        }
        hasPro = purchases.values.any { it.isProSubscription } || purchases.containsKey(SKU_VIP)
        updateSubscription()
    }

    fun purchasedTasker() = hasPro || purchases.containsKey(SKU_TASKER)

    fun purchasedThemes() = hasPro || purchases.containsKey(SKU_THEMES)

    var hasPro = false
        get() {
            @Suppress("KotlinConstantConditions")
            return BuildConfig.FLAVOR == "generic"
                    || (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false))
                    || hasTasksAccount
                    || field
        }
        private set

    suspend fun updateTasksAccount() {
        hasTasksAccount = caldavDao.getAccounts(TYPE_TASKS).any {
            it.isTasksSubscription(context)
        }
    }

    fun getPurchase(sku: String) = purchases[sku]

    private fun updateSubscription() {
        subscription.value = purchases
                .values
                .filter { it.isProSubscription }
                .sortedWith { l, r ->
                    r.isMonthly.compareTo(l.isMonthly)
                            .takeIf { it != 0 }?.let { return@sortedWith it }
                    l.isCanceled.compareTo(r.isCanceled)
                            .takeIf { it != 0 }?.let { return@sortedWith it }
                    r.subscriptionPrice!!.compareTo(l.subscriptionPrice!!)
                }
                .firstOrNull()
    }

    fun unsubscribe(context: Context): Boolean {
        subscription.value?.let {
            context.openUri(R.string.manage_subscription_url, it.sku)
        }
        return false
    }

    companion object {
        private const val SKU_VIP = "vip"
        const val SKU_TASKER = "tasker"
        const val SKU_THEMES = "themes"
    }

    init {
        val runnable = { verifyAndAdd(preferences.purchases) }
        val mainLooper = context.mainLooper
        if (mainLooper.isCurrentThread) {
            runnable()
        } else {
            Handler(context.mainLooper).post(runnable)
        }
    }
}
