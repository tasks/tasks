package org.tasks.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.billing.Inventory
import org.tasks.billing.Purchase
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountResponse
import org.tasks.compose.settings.AccountData
import org.tasks.compose.settings.ProCardState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences.Companion.cachedAccountData
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProCardViewModel @Inject constructor(
    private val caldavDao: CaldavDao,
    private val inventory: Inventory,
    private val provider: CaldavClientProvider,
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val accounts: StateFlow<List<CaldavAccount>> = caldavDao.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val subscription: StateFlow<Purchase?> = inventory.subscription
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), inventory.subscription.value)

    private val _accountData = MutableStateFlow<AccountData?>(null)
    private val _isLoading = MutableStateFlow(false)

    val proCardState: StateFlow<ProCardState> = combine(
        accounts,
        subscription,
        _accountData,
        _isLoading,
    ) { accountList, sub, accountData, isLoading ->
        deriveProCardState(accountList, sub, accountData, isLoading)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        deriveProCardState(emptyList(), null, null, false)
    )

    val filteredAccounts: StateFlow<List<CaldavAccount>> = combine(
        accounts,
        proCardState,
    ) { accountList, cardState ->
        if (cardState is ProCardState.TasksOrgAccount) {
            accountList.filter { !it.isTasksOrg }
        } else {
            accountList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            loadCachedAccountData()
            accounts.first { it.isNotEmpty() }
                .firstOrNull { it.isTasksOrg }
                ?.let { fetchAccountData(it) }
        }
    }

    private suspend fun loadCachedAccountData() {
        val raw = tasksPreferences.get(cachedAccountData, "")
        if (raw.isNotBlank()) {
            try {
                _accountData.value = json.decodeFromString<TasksAccountResponse>(raw).toAccountData()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun fetchAccountData(account: CaldavAccount) {
        _isLoading.value = true
        try {
            val raw = provider.forTasksAccount(account).getAccount() ?: return
            tasksPreferences.set(cachedAccountData, raw)
            _accountData.value = json.decodeFromString<TasksAccountResponse>(raw).toAccountData()
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun deriveProCardState(
        accountList: List<CaldavAccount>,
        sub: Purchase?,
        accountData: AccountData?,
        isLoading: Boolean,
    ): ProCardState {
        val tasksOrgAccount = accountList.firstOrNull { it.isTasksOrg }
        return when {
            tasksOrgAccount != null -> ProCardState.TasksOrgAccount(tasksOrgAccount, isLoading && accountData == null, accountData)
            sub?.isTasksSubscription == true -> ProCardState.SignIn
            !IS_GENERIC && sub != null -> ProCardState.Subscribed(
                isMonthly = sub.isMonthly,
                subscriptionPrice = sub.subscriptionPrice ?: 0,
            )
            !IS_GENERIC -> ProCardState.Upgrade
            else -> ProCardState.Donate
        }
    }

    companion object {
        private fun TasksAccountResponse.toAccountData() = AccountData(
            createdAt = createdAt,
            subscriptionFree = subscription?.free ?: true,
            subscriptionProvider = subscription?.provider,
            subscriptionExpiration = subscription?.expiration,
        )
    }
}
