package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.compose.settings.TodoistAccountState
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.ConnectionException
import org.tasks.http.HttpException
import org.tasks.http.UnauthorizedException
import org.tasks.resources.getString
import org.tasks.security.Encryption
import org.tasks.service.TaskDeleter
import org.tasks.sync.todoist.TodoistClientProvider
import org.tasks.sync.todoist.TodoistResourceTypes
import org.tasks.sync.todoist.TodoistService
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.invalid_username_or_password
import tasks.kmp.generated.resources.network_error
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.todoist

@OptIn(ExperimentalCoroutinesApi::class)
open class TodoistAccountSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val clientProvider: TodoistClientProvider,
    private val encryption: Encryption,
    private val taskDeleter: TaskDeleter,
    private val reporting: Reporting,
) : ViewModel() {

    private val writer = TodoistAccountWriter(caldavDao, reporting)

    private val accountId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(TodoistAccountState())
    val state: StateFlow<TodoistAccountState> = _state

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

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
        _state.value = TodoistAccountState(
            displayName = account.name.orEmpty(),
            account = account,
        )
    }

    fun setToken(token: String) {
        _state.update { it.copy(token = token, tokenError = null) }
    }

    fun setDisplayName(name: String) {
        _state.update { it.copy(displayName = name) }
    }

    fun dismissSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun save() {
        if (_state.value.isLoading) {
            return
        }
        viewModelScope.launch {
            val current = _state.value
            val existing = current.account
            val token = current.token.trim()
            val name = current.displayName.trim()
            if (token.isEmpty()) {
                saveWithoutNewToken(existing, name)
            } else {
                applyNewToken(existing, token, name)
            }
        }
    }

    private suspend fun saveWithoutNewToken(existing: CaldavAccount?, name: String) {
        if (existing == null) {
            _state.update { it.copy(tokenError = getString(Res.string.password_required)) }
            return
        }
        caldavDao.update(existing.apply { this.name = name.ifEmpty { null } })
        _finished.value = true
    }

    private suspend fun applyNewToken(existing: CaldavAccount?, token: String, name: String) {
        _state.update { it.copy(isLoading = true, tokenError = null, snackbar = null) }
        val failure = runCatching { storeToken(existing, token, name) }.exceptionOrNull()
        if (failure is CancellationException) {
            throw failure
        }
        if (failure != null) {
            if (!failure.isRecognisedSignInFailure()) {
                Logger.e(failure) { "Todoist sign-in failed" }
                reporting.reportException(failure)
            }
            val tokenError = failure.signInTokenError()
            val snackbar = if (tokenError == null) failure.signInSnackbar() else null
            _state.update { it.copy(tokenError = tokenError, snackbar = snackbar) }
        }
        _state.update { it.copy(isLoading = false) }
    }

    private suspend fun storeToken(existing: CaldavAccount?, token: String, name: String) {
        val accountName = verify(token) ?: name.ifEmpty { getString(Res.string.todoist) }
        val encrypted = encryption.encrypt(token) ?: error("Could not encrypt token")
        if (existing == null) {
            writer.insertAccount(name.ifEmpty { accountName }, encrypted)
        } else {
            writer.reauthenticate(existing, encrypted, name)
        }
        _finished.value = true
    }

    private suspend fun verify(token: String): String? {
        val response = clientProvider
            .getService(token)
            .sync(
                syncToken = TodoistService.FULL_SYNC,
                resourceTypes = listOf(TodoistResourceTypes.PROJECTS),
            )
        return response.projects.firstOrNull { it.isInboxProject }?.name
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.account?.let { taskDeleter.delete(it) }
            onComplete()
        }
    }
}

private class TodoistAccountWriter(
    private val caldavDao: CaldavDao,
    private val reporting: Reporting,
) {
    suspend fun insertAccount(accountName: String, encrypted: String) {
        caldavDao.insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_TODOIST,
                uuid = UUIDHelper.newUUID(),
                name = accountName,
                username = accountName,
                password = encrypted,
            )
        )
        reporting.logEvent(
            AnalyticsEvents.SYNC_ADD_ACCOUNT,
            AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_TODOIST,
        )
    }

    suspend fun reauthenticate(
        existing: CaldavAccount,
        encrypted: String,
        name: String,
    ) {
        caldavDao.setPassword(existing.id, encrypted)
        caldavDao.setSyncToken(existing.id, null)
        caldavDao.setError(existing.id, "")
        if (name.isNotEmpty() && name != existing.name) {
            caldavDao.update(existing.apply { this.name = name })
        }
    }
}

private fun Throwable.isRecognisedSignInFailure() =
    this is UnauthorizedException || this is HttpException || this is ConnectionException

private suspend fun Throwable.signInTokenError(): String? = when {
    this is UnauthorizedException -> getString(Res.string.invalid_username_or_password)
    this is HttpException && (code == 401 || code == 403) ->
        getString(Res.string.invalid_username_or_password)
    else -> null
}

private suspend fun Throwable.signInSnackbar(): String = when (this) {
    is HttpException -> message ?: getString(Res.string.network_error)
    is ConnectionException -> getString(Res.string.network_error)
    else -> getString(
        Res.string.error_adding_account,
        message ?: this::class.simpleName ?: "",
    )
}
