package org.tasks.billing

import android.content.Context
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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
import org.tasks.resources.getString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.url_sponsor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Inventory @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    private val preferences: Preferences,
    private val playPurchases: PlayPurchases,
    private val subscriptionProvider: SubscriptionProvider,
    private val stripePurchases: Lazy<StripePurchases>,
    private val localBroadcastManager: LocalBroadcastManager,
    private val caldavDao: CaldavDao,
) : PurchaseState {
    private val _subscription = MutableStateFlow<SubscriptionProvider.SubscriptionInfo?>(null)
    val subscription: StateFlow<SubscriptionProvider.SubscriptionInfo?> =
        _subscription.asStateFlow()

    private val _tasksAccount = MutableStateFlow(false)
    val tasksAccount: StateFlow<Boolean> = _tasksAccount.asStateFlow()

    val purchases: Map<String, PlayPurchase>
        get() = playPurchases.state.value.purchases

    override val hasTasksAccount: Boolean
        get() = _tasksAccount.value

    override val hasTasksSubscription: Boolean
        get() = subscriptionProvider.current?.isTasksSubscription == true || hasTasksAccount

    val hasSubscription: Boolean
        get() = playPurchases.hasPro ||
                _subscription.value != null ||
                subscriptionProvider.current != null ||
                hasTasksAccount

    val begForMoney: Boolean
        get() = if (IS_GENERIC) !hasSubscription else !hasPro

    override fun purchasedThemes() = hasPro || playPurchases.purchasedThemes

    @Suppress("KotlinConstantConditions")
    override val hasPro: Boolean
        get() = IS_GENERIC ||
                (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false)) ||
                hasSubscription

    suspend fun updateTasksAccount() {
        _tasksAccount.value = caldavDao.getAccounts(TYPE_TASKS, TYPE_CALDAV).any {
            it.isTasksSubscription()
        }
    }

    suspend fun awaitVerification(): Boolean = subscriptionProvider.awaitVerification()

    fun getPurchase(sku: String) = playPurchases.get(sku)

    suspend fun unsubscribe(context: Context) {
        val subscription = _subscription.value ?: return
        when (subscription.provider) {
            EntitlementProvider.STRIPE ->
                stripePurchases.get().portalUrl()?.let { context.openUri(it) }
            EntitlementProvider.GITHUB_SPONSOR ->
                context.openUri(getString(Res.string.url_sponsor))
            EntitlementProvider.PLAY, null ->
                context.openUri(R.string.manage_subscription_url, subscription.sku)
        }
    }

    init {
        scope.launch(Dispatchers.Main.immediate) {
            playPurchases.updates
                .drop(1)
                .collect { localBroadcastManager.broadcastPurchasesUpdated() }
        }
        scope.launch(Dispatchers.Main.immediate) {
            subscriptionProvider.subscription.collect {
                if (_subscription.value != it) {
                    _subscription.value = it
                    localBroadcastManager.broadcastPurchasesUpdated()
                }
            }
        }
    }
}
