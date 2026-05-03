package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.settings.CaldavAccountState
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.jobs.BackgroundWork
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncSource
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.invalid_username_or_password
import tasks.kmp.generated.resources.network_error
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.url_host_name_required
import tasks.kmp.generated.resources.url_invalid_scheme
import tasks.kmp.generated.resources.url_required
import tasks.kmp.generated.resources.username_required
import java.net.ConnectException
import java.net.IDN
import java.net.URI
import java.net.URISyntaxException

@OptIn(ExperimentalCoroutinesApi::class)
open class CaldavAccountSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val caldavClientProvider: CaldavClientProvider,
    private val encryption: KeyStoreEncryption,
    private val taskDeleter: TaskDeleter,
    private val backgroundWork: BackgroundWork,
    private val reporting: Reporting,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(CaldavAccountState())
    val state: StateFlow<CaldavAccountState> = _state

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
        _state.value = CaldavAccountState(
            url = account.url.orEmpty(),
            username = account.username.orEmpty(),
            password = "",
            displayName = account.name.orEmpty(),
            serverType = account.serverType,
            account = account,
        )
    }

    fun setUrl(value: String) {
        _state.update { it.copy(url = value, urlError = null) }
    }

    fun setUsername(value: String) {
        _state.update { it.copy(username = value, usernameError = null) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun setDisplayName(value: String) {
        _state.update { it.copy(displayName = value) }
    }

    fun setServerType(value: Int) {
        _state.update { it.copy(serverType = value) }
    }

    fun dismissSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun save(onComplete: () -> Unit) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            if (validate()) {
                submit(onComplete)
            }
        }
    }

    private suspend fun validate(): Boolean {
        val s = _state.value
        val urlValue = s.url.trim()
        val usernameValue = s.username.trim()
        val passwordValue = s.password
        val isEditing = s.account != null

        var urlError: String? = null
        var usernameError: String? = null
        var passwordError: String? = null

        if (urlValue.isEmpty()) {
            urlError = getString(Res.string.url_required)
        } else {
            try {
                val uri = URI(urlValue)
                val scheme = uri.scheme
                if (scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)) {
                    val host = uri.host
                    if (host.isNullOrEmpty()) {
                        urlError = getString(Res.string.url_host_name_required)
                    } else {
                        try {
                            IDN.toASCII(host)
                            URI(scheme, null, host, uri.port, uri.path, null, null)
                        } catch (e: URISyntaxException) {
                            urlError = e.localizedMessage
                        } catch (_: Exception) {
                            // IDN conversion non-fatal
                        }
                    }
                } else {
                    urlError = getString(Res.string.url_invalid_scheme)
                }
            } catch (_: URISyntaxException) {
                urlError = getString(Res.string.url_invalid_scheme)
            }
        }

        if (usernameValue.isEmpty()) {
            usernameError = getString(Res.string.username_required)
        }

        if (!isEditing && passwordValue.isEmpty()) {
            passwordError = getString(Res.string.password_required)
        }

        val failed = urlError != null || usernameError != null || passwordError != null
        if (failed) {
            _state.update {
                it.copy(
                    urlError = urlError,
                    usernameError = usernameError,
                    passwordError = passwordError,
                )
            }
        }
        return !failed
    }

    private suspend fun submit(onComplete: () -> Unit) {
        val s = _state.value
        val urlValue = s.url.trim()
        val usernameValue = s.username.trim()
        val passwordValue = s.password
        val nameValue = s.displayName.trim()
        val effectiveName = nameValue.ifEmpty { usernameValue }
        val isEditing = s.account != null

        if (isEditing) {
            val currentAccount = s.account!!
            val credentialsChanged = urlValue != (currentAccount.url ?: "") ||
                    usernameValue != (currentAccount.username ?: "") ||
                    passwordValue.isNotEmpty()

            if (credentialsChanged) {
                val effectivePassword = passwordValue.ifEmpty {
                    encryption.decrypt(currentAccount.password) ?: ""
                }
                _state.update { it.copy(isLoading = true) }
                try {
                    val principal = withContext(Dispatchers.IO) {
                        caldavClientProvider
                            .forUrl(urlValue, usernameValue, effectivePassword)
                            .homeSet(usernameValue, effectivePassword)
                    }
                    updateAccount(currentAccount, principal, effectiveName, usernameValue, effectivePassword, s.serverType)
                    onComplete()
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    _state.update { it.copy(isLoading = false) }
                }
            } else if (s.hasChanges) {
                updateAccount(currentAccount, currentAccount.url, effectiveName, usernameValue, null, s.serverType)
                onComplete()
            } else {
                onComplete()
            }
        } else {
            _state.update { it.copy(isLoading = true) }
            try {
                val principal = withContext(Dispatchers.IO) {
                    caldavClientProvider
                        .forUrl(urlValue, usernameValue, passwordValue)
                        .homeSet(usernameValue, passwordValue)
                }
                addAccount(principal, effectiveName, usernameValue, passwordValue, s.serverType)
                reporting.logEvent(
                    "sync_add_account",
                    "type" to Constants.SYNC_TYPE_CALDAV,
                )
                onComplete()
                backgroundWork.sync(SyncSource.ACCOUNT_ADDED)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun addAccount(
        principal: String,
        name: String,
        username: String,
        password: String,
        serverType: Int,
    ) {
        caldavDao.insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_CALDAV,
                name = name,
                url = principal,
                username = username,
                password = encryption.encrypt(password),
                uuid = UUIDHelper.newUUID(),
                serverType = serverType,
            )
        )
    }

    private suspend fun updateAccount(
        current: CaldavAccount,
        url: String?,
        name: String,
        username: String,
        newPassword: String?,
        serverType: Int,
    ) {
        caldavDao.update(
            current.copy(
                name = name,
                url = url,
                username = username,
                password = newPassword?.let { encryption.encrypt(it) } ?: current.password,
                serverType = serverType,
                error = "",
            )
        )
    }

    private suspend fun handleError(e: Exception) {
        Logger.e(e) { "CalDAV account operation failed" }
        _state.update {
            it.copy(
                snackbar = when (e) {
                    is HttpException -> {
                        if (e.statusCode == 401) getString(Res.string.invalid_username_or_password)
                        else e.message
                    }
                    is DisplayableException -> getString(e.resource)
                    is ConnectException -> getString(Res.string.network_error)
                    else -> getString(Res.string.error_adding_account, e.message ?: "")
                }
            )
        }
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.account?.let { taskDeleter.delete(it) }
            onComplete()
        }
    }
}
