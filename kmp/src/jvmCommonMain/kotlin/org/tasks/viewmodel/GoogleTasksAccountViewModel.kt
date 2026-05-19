package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.service.TaskDeleter

@OptIn(ExperimentalCoroutinesApi::class)
open class GoogleTasksAccountViewModel(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(GoogleTasksAccountState())
    val state: StateFlow<GoogleTasksAccountState> = _state

    init {
        viewModelScope.launch {
            accountId
                .filterNotNull()
                .flatMapLatest { caldavDao.watchAccount(it) }
                .collect { account ->
                    _state.update { it.copy(account = account) }
                }
        }
    }

    fun setAccount(account: CaldavAccount) {
        accountId.value = account.id
        _state.update { it.copy(account = account) }
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.account?.let { taskDeleter.delete(it) }
            onComplete()
        }
    }
}

data class GoogleTasksAccountState(
    val account: CaldavAccount? = null,
) {
    val error: String?
        get() = account?.error?.takeIf { it.isNotBlank() }

    val isUnauthorized: Boolean
        get() = account?.error?.startsWith("401 Unauthorized", ignoreCase = true) == true
}
