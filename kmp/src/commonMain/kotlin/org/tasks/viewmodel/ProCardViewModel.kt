package org.tasks.viewmodel

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tasks.PlatformConfiguration
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.billing.formatCredit
import org.tasks.billing.stripeTerms
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.TasksAccountResponse
import org.tasks.compose.settings.AccountData
import org.tasks.compose.settings.ProCardState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

data class SettingsAccountsState(
    val accounts: List<CaldavAccount>,
    val proCard: ProCardState,
)

open class ProCardViewModel(
    private val caldavDao: CaldavDao,
    private val subscriptionProvider: SubscriptionProvider,
    private val tasksPreferences: TasksPreferences,
    private val accountDataRepository: TasksAccountDataRepository,
    private val serverEnvironment: TasksServerEnvironment,
    private val platformConfiguration: PlatformConfiguration,
) : ViewModel() {

    private val accounts: StateFlow<List<CaldavAccount>?> = caldavDao.watchAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _accountData = MutableStateFlow<AccountData?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _environmentLabel = MutableStateFlow<String?>(null)
    private val _formattedPrice = MutableStateFlow<String?>(null)
    val environmentLabel: StateFlow<String?> = _environmentLabel

    val state: StateFlow<SettingsAccountsState?> = combine(
        accounts,
        subscriptionProvider.subscription,
        _accountData,
        _isLoading,
        _formattedPrice,
    ) { accountList, sub, accountData, isLoading, formattedPrice ->
        accountList?.let {
            val proCard = deriveProCardState(it, sub, accountData, isLoading, formattedPrice)
            SettingsAccountsState(
                accounts = if (proCard is ProCardState.TasksOrgAccount) {
                    it.filter { account -> !account.isTasksOrg }
                } else {
                    it
                },
                proCard = proCard,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val proCardState: StateFlow<ProCardState> = state
        .map { it?.proCard ?: unloadedProCardState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), unloadedProCardState)

    val filteredAccounts: StateFlow<List<CaldavAccount>> = state
        .map { it?.accounts.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val unloadedProCardState: ProCardState
        get() = deriveProCardState(emptyList(), null, null, false, null)

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
            accounts.filterNotNull().first { it.isNotEmpty() }
                .firstOrNull { it.isTasksOrg }
                ?.let { fetchAccountData(it) }
        }
        viewModelScope.launch {
            subscriptionProvider.checkTier()
        }
        viewModelScope.launch {
            subscriptionProvider.subscription.collectLatest { sub ->
                if (sub != null) {
                    val cacheKey = stringPreferencesKey("fmt_${sub.sku}")
                    val cached = tasksPreferences.get(cacheKey, "")
                    _formattedPrice.value = cached.ifBlank { null }
                    try {
                        val price = subscriptionProvider.getFormattedPrice(sub.sku)
                        if (price == null) {
                            _formattedPrice.value = cached.ifBlank { "" }
                        } else {
                            if (price != cached) {
                                tasksPreferences.set(cacheKey, price)
                            }
                            _formattedPrice.value = price
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to get formatted price" }
                        _formattedPrice.value = cached.ifBlank { "" }
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
        val credit = creditToShow(sub, accountData)
        val terms = sub?.terms ?: accountData?.terms
        return when {
            tasksOrgAccount != null -> ProCardState.TasksOrgAccount(
                tasksOrgAccount,
                isLoading && accountData == null,
                accountData,
                credit,
                terms,
                formattedPrice,
                sub?.isMonthly == true,
            )
            sub?.isTasksSubscription == true -> ProCardState.SignIn(
                credit = credit,
                terms = terms,
                formattedPrice = formattedPrice,
                isMonthly = sub.isMonthly,
            )
            hasBilling && sub != null -> ProCardState.Subscribed(
                isMonthly = sub.isMonthly,
                formattedPrice = formattedPrice,
                credit = credit,
                terms = terms,
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
            hasSubscription = hasAnySubscription,
            creditCents = creditCents,
            creditCurrency = creditCurrency,
            terms = stripeTerms(),
        )
    }
}

internal fun creditToShow(
    sub: SubscriptionProvider.SubscriptionInfo?,
    accountData: AccountData?,
): String? {
    if (sub == null && accountData?.hasSubscription != true) {
        return null
    }
    return formatCredit(accountData?.creditCents, accountData?.creditCurrency)
        ?: formatCredit(sub?.terms?.creditCents, sub?.terms?.creditCurrency)
}
