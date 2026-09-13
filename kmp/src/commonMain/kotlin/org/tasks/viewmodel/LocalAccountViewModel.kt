package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.jobs.BackgroundWork
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.name_cannot_be_empty

open class LocalAccountViewModel(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val backgroundWork: BackgroundWork,
    private val purchaseState: PurchaseState,
) : ViewModel() {
    private val accountId = MutableStateFlow<Long?>(null)
    val displayName = MutableStateFlow("")
    val nameError = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val account: StateFlow<CaldavAccount?> = accountId
        .filterNotNull()
        .flatMapLatest { caldavDao.watchAccount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val taskCount: StateFlow<Int> = account
        .flatMapLatest { account ->
            account?.uuid?.let { caldavDao.watchTaskCount(it) } ?: flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val hasChanges: StateFlow<Boolean> = combine(displayName, account) { name, current ->
        current != null && name.trim() != (current.name ?: "")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAccount(account: CaldavAccount) {
        accountId.value = account.id
        displayName.value = account.name.orEmpty()
        nameError.value = null
    }

    open fun setDisplayName(name: String) {
        displayName.value = name
        nameError.value = null
    }

    fun save(onComplete: () -> Unit) {
        val name = displayName.value.trim()
        if (name.isBlank()) {
            viewModelScope.launch {
                nameError.value = getString(Res.string.name_cannot_be_empty)
            }
            return
        }
        viewModelScope.launch {
            account.value?.let {
                caldavDao.update(it.copy(name = name))
            }
            onComplete()
        }
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            account.value?.let { taskDeleter.delete(it) }
            onComplete()
        }
    }

    suspend fun canMigrate(
        onPurchaseRequired: () -> Unit,
        onSignInRequired: () -> Unit,
    ): Boolean {
        if (!purchaseState.hasTasksSubscription) {
            onPurchaseRequired()
            return false
        }
        val tasksAccount = caldavDao
            .getAccounts(CaldavAccount.TYPE_TASKS)
            .firstOrNull()
        if (tasksAccount == null) {
            onSignInRequired()
            return false
        }
        return true
    }

    fun confirmMigration(onComplete: () -> Unit) {
        val localAccount = account.value ?: return
        viewModelScope.launch {
            val tasksAccount = caldavDao
                .getAccounts(CaldavAccount.TYPE_TASKS)
                .firstOrNull()
                ?: return@launch
            backgroundWork.migrateLocalTasks(localAccount, tasksAccount)
            onComplete()
        }
    }
}
