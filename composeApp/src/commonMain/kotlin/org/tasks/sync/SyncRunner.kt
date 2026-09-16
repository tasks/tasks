package org.tasks.sync

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.tasks.billing.SubscriptionProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount

private const val TAG = "SyncRunner"

class SyncRunner(
    private val scope: CoroutineScope,
    private val caldavDao: CaldavDao,
    private val subscriptionProvider: () -> SubscriptionProvider,
    private val pass: suspend (SyncPass) -> Unit,
) {
    class SyncPass(
        val accounts: List<CaldavAccount>,
        val hasPro: Boolean,
        val googleAndMicrosoftPro: Boolean,
    ) {
        fun accounts(vararg types: Int): List<CaldavAccount> = accounts.filter { it.accountType in types }
    }

    private val mutex = Mutex()
    private val pending = MutableStateFlow(false)

    fun sync(source: SyncSource) {
        scope.launch {
            if (!mutex.tryLock()) {
                pending.value = true
                return@launch
            }
            try {
                do {
                    pending.value = false
                    runPass(source)
                } while (pending.getAndUpdate { false })
            } finally {
                mutex.unlock()
            }
        }
    }

    private suspend fun runPass(source: SyncSource) {
        val subscriptionProvider = subscriptionProvider()
        val accounts = caldavDao.getAccounts()
        val hasTasksOrg = accounts.any { it.isTasksOrg }
        if (!hasTasksOrg && !subscriptionProvider.awaitVerification()) {
            Logger.e(TAG) { "Could not confirm subscription, syncing without pro" }
        }
        val hasPro = hasTasksOrg || subscriptionProvider.subscription.first() != null
        Logger.d(TAG) { "sync source=$source accounts=${accounts.size} pro=$hasPro" }
        pass(
            SyncPass(
                accounts = accounts,
                hasPro = hasPro,
                googleAndMicrosoftPro = hasPro || !subscriptionProvider.googleAndMicrosoftRequirePro,
            )
        )
    }
}
