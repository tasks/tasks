package org.tasks.billing

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.isTasksSubscription
import org.tasks.extensions.Context.openUri
import org.tasks.injection.ApplicationScope
import org.tasks.preferences.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Inventory @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    private val preferences: Preferences,
    private val playPurchases: PlayPurchases,
    private val entitlement: PurchaseEntitlement,
    private val localBroadcastManager: LocalBroadcastManager,
    private val caldavDao: CaldavDao,
) : PurchaseState {
    val subscription = MutableLiveData<PlayPurchase?>()

    val currentSubscription: PlayPurchase?
        get() = playPurchases.state.value.subscription

    @Volatile
    private var entitled = false

    val purchases: Map<String, PlayPurchase>
        get() = playPurchases.state.value.purchases

    override var hasTasksAccount = false
        private set

    override val hasTasksSubscription: Boolean
        get() = currentSubscription?.isTasksSubscription == true || hasTasksAccount

    val hasSubscription: Boolean
        get() = playPurchases.hasPro || entitled || entitlement.hasPro.value || hasTasksAccount

    val begForMoney: Boolean
        get() = if (IS_GENERIC) !hasSubscription else !hasPro

    override fun purchasedThemes() = hasPro || playPurchases.purchasedThemes

    @Suppress("KotlinConstantConditions")
    override val hasPro: Boolean
        get() = IS_GENERIC ||
                (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false)) ||
                hasTasksAccount ||
                playPurchases.hasPro ||
                entitled ||
                entitlement.hasPro.value

    suspend fun updateTasksAccount() {
        hasTasksAccount = caldavDao.getAccounts(TYPE_TASKS, TYPE_CALDAV).any {
            it.isTasksSubscription()
        }
    }

    suspend fun awaitVerification(): Boolean = entitlement.awaitReady()

    fun getPurchase(sku: String) = playPurchases.get(sku)

    fun unsubscribe(context: Context): Boolean {
        currentSubscription?.let {
            context.openUri(R.string.manage_subscription_url, it.sku)
        }
        return false
    }

    init {
        scope.launch {
            playPurchases.load()
        }
        scope.launch(Dispatchers.Main.immediate) {
            playPurchases.updates.collect {
                subscription.value = playPurchases.state.value.subscription
                localBroadcastManager.broadcastPurchasesUpdated()
            }
        }
        scope.launch(Dispatchers.Main.immediate) {
            entitlement.hasPro.collect {
                entitled = it
                localBroadcastManager.broadcastPurchasesUpdated()
            }
        }
    }
}
