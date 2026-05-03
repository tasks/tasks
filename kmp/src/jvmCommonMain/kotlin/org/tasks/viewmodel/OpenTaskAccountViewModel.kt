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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.name_cannot_be_empty

open class OpenTaskAccountViewModel(
    private val caldavDao: CaldavDao,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    val displayName = MutableStateFlow("")
    val nameError = MutableStateFlow<String?>(null)
    val serverType = MutableStateFlow(SERVER_UNKNOWN)

    @OptIn(ExperimentalCoroutinesApi::class)
    val account: StateFlow<CaldavAccount?> = accountId
        .filterNotNull()
        .flatMapLatest { caldavDao.watchAccount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hasChanges: StateFlow<Boolean> = combine(
        displayName, serverType, account
    ) { name, serverType, current ->
        current != null && (
            name.trim() != (current.name ?: "") ||
            serverType != current.serverType
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAccount(account: CaldavAccount) {
        accountId.value = account.id
        displayName.value = account.name.orEmpty()
        nameError.value = null
        serverType.value = account.serverType
    }

    fun setDisplayName(name: String) {
        displayName.value = name
        nameError.value = null
    }

    fun setServerType(value: Int) {
        serverType.value = value
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
                caldavDao.update(it.copy(name = name, serverType = serverType.value))
            }
            onComplete()
        }
    }
}
