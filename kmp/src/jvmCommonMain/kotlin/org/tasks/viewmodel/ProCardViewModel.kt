package org.tasks.viewmodel

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tasks.PlatformConfiguration
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.TasksAccountResponse
import org.tasks.compose.settings.AccountData
import org.tasks.compose.settings.ProCardState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

open class ProCardViewModel(
    private val caldavDao: CaldavDao,
    private val subscriptionProvider: SubscriptionProvider,
    private val tasksPreferences: TasksPreferences,
    private val accountDataRepository: TasksAccountDataRepository,
    private val serverEnvironment: TasksServerEnvironment,
    private val platformConfiguration: PlatformConfiguration,
) : ViewModel() {

    private val accounts: StateFlow<List<CaldavAccount>> = caldavDao.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _accountData = MutableStateFlow<AccountData?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _environmentLabel = MutableStateFlow<String?>(null)
    private val _formattedPrice = MutableStateFlow<String?>(null)
    val environmentLabel: StateFlow<String?> = _environmentLabel

    val proCardState: StateFlow<ProCardState> = combine(
        accounts,
        subscriptionProvider.subscription,
        _accountData,
        _isLoading,
        _formattedPrice,
    ) { accountList, sub, accountData, isLoading, formattedPrice ->
        deriveProCardState(accountList, sub, accountData, isLoading, formattedPrice)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        deriveProCardState(emptyList(), null, null, false, null)
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
            val env = serverEnvironment.getEnvironment()
            _environmentLabel.value = when (env) {
                TasksServerEnvironment.ENV_STAGING -> "Staging"
                TasksServerEnvironment.ENV_DEV -> "Development"
                else -> null
            }
            accountDataRepository.getAccountResponse()
                ?.toAccountData()
                ?.let { _accountData.value = it }
            accounts.first { it.isNotEmpty() }
                .firstOrNull { it.isTasksOrg }
                ?.let { fetchAccountData(it) }
        }
        viewModelScope.launch {
            subscriptionProvider.subscription.collectLatest { sub ->
                if (sub != null) {
                    val cacheKey = stringPreferencesKey("fmt_${sub.sku}")
                    val cached = tasksPreferences.get(cacheKey, "")
                    if (cached.isNotBlank()) {
                        _formattedPrice.value = cached
                    } else {
                        _formattedPrice.value = null
                        try {
                            val price = subscriptionProvider.getFormattedPrice(sub.sku)
                            if (price != null) {
                                tasksPreferences.set(cacheKey, price)
                                _formattedPrice.value = price
                            } else {
                                _formattedPrice.value = ""
                            }
                        } catch (e: Exception) {
                            Logger.e(e) { "Failed to get formatted price" }
                            _formattedPrice.value = ""
                        }
                    }
                } else {
                    _formattedPrice.value = null
                }
            }
        }
    }

    private suspend fun fetchAccountData(account: CaldavAccount) {
        _isLoading.value = true
        try {
            _accountData.value = accountDataRepository.fetchAndCache(account)?.toAccountData()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to fetch account data" }
        } finally {
            _isLoading.value = false
        }
    }

    private val hasBilling: Boolean
        get() = platformConfiguration.billingProvider != null

    private fun deriveProCardState(
        accountList: List<CaldavAccount>,
        sub: SubscriptionProvider.SubscriptionInfo?,
        accountData: AccountData?,
        isLoading: Boolean,
        formattedPrice: String?,
    ): ProCardState {
        val tasksOrgAccount = accountList.firstOrNull { it.isTasksOrg }
        return when {
            tasksOrgAccount != null -> ProCardState.TasksOrgAccount(tasksOrgAccount, isLoading && accountData == null, accountData)
            sub?.isTasksSubscription == true -> ProCardState.SignIn
            hasBilling && sub != null -> ProCardState.Subscribed(
                isMonthly = sub.isMonthly,
                formattedPrice = formattedPrice,
            )
            hasBilling -> ProCardState.Upgrade
            else -> ProCardState.Donate
        }
    }

    companion object {
        private fun TasksAccountResponse.toAccountData() = AccountData(
            createdAt = createdAt,
            subscriptionFree = subscription?.free ?: true,
            subscriptionProvider = subscription?.provider,
            subscriptionExpiration = subscription?.expiration,
            guest = guest,
        )
    }
}
