package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.etebase.client.exceptions.ConnectionException
import com.etebase.client.exceptions.UnauthorizedException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.compose.settings.EtebaseAccountState
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.jobs.BackgroundWork
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncSource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.etebase_url
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
open class EtebaseAccountSettingsViewModel(
    private val caldavDao: CaldavDao,
    private val clientProvider: EtebaseClientProvider,
    private val encryption: KeyStoreEncryption,
    private val taskDeleter: TaskDeleter,
    private val backgroundWork: BackgroundWork,
    private val reporting: Reporting,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(EtebaseAccountState())
    val state: StateFlow<EtebaseAccountState> = _state

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

    suspend fun setAccount(account: CaldavAccount) {
        val defaultUrl = getString(Res.string.etebase_url)
        val hasCustomUrl = !account.url.isNullOrBlank() && account.url != defaultUrl
        accountId.value = account.id
        _state.value = EtebaseAccountState(
            url = account.url.orEmpty(),
            username = account.username.orEmpty(),
            password = "",
            displayName = account.name.orEmpty(),
            account = account,
            showUrl = hasCustomUrl,
            hasCustomUrl = hasCustomUrl,
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

    fun setShowUrl(value: Boolean) {
        _state.update { it.copy(showUrl = value) }
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

        if (s.showUrl && urlValue.isNotEmpty()) {
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
        val usernameValue = s.username.trim()
        val passwordValue = s.password
        val nameValue = s.displayName.trim()
        val effectiveName = nameValue.ifEmpty { usernameValue }
        val isEditing = s.account != null
        val urlValue = if (s.showUrl) s.url.trim().ifEmpty { getString(Res.string.etebase_url) } else getString(Res.string.etebase_url)

        if (isEditing) {
            val currentAccount = s.account!!
            val credentialsChanged = urlValue != (currentAccount.url ?: "") ||
                    usernameValue != (currentAccount.username ?: "") ||
                    passwordValue.isNotEmpty()

            if (credentialsChanged) {
                _state.update { it.copy(isLoading = true) }
                try {
                    val currentSession = encryption.decrypt(currentAccount.password) ?: ""
                    val session = if (passwordValue.isEmpty()) {
                        clientProvider
                            .forUrl(
                                url = urlValue,
                                username = usernameValue,
                                password = null,
                                session = currentSession,
                                foreground = true,
                            )
                            .getSession()
                    } else {
                        clientProvider
                            .forUrl(
                                url = urlValue,
                                username = usernameValue,
                                password = passwordValue,
                                foreground = true,
                            )
                            .getSession()
                    }
                    updateAccount(currentAccount, urlValue, effectiveName, usernameValue, session)
                    onComplete()
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    _state.update { it.copy(isLoading = false) }
                }
            } else if (s.hasChanges) {
                updateAccount(currentAccount, currentAccount.url, effectiveName, usernameValue, null)
                onComplete()
            } else {
                onComplete()
            }
        } else {
            _state.update { it.copy(isLoading = true) }
            try {
                val session = clientProvider
                    .forUrl(
                        url = urlValue,
                        username = usernameValue,
                        password = passwordValue,
                        foreground = true,
                    )
                    .getSession()
                addAccount(urlValue, effectiveName, usernameValue, session)
                reporting.logEvent(
                    "sync_add_account",
                    "type" to Constants.SYNC_TYPE_ETEBASE,
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
        url: String,
        name: String,
        username: String,
        session: String,
    ) {
        caldavDao.insert(
            CaldavAccount(
                accountType = CaldavAccount.TYPE_ETEBASE,
                name = name,
                url = url,
                username = username,
                password = encryption.encrypt(session),
                uuid = UUIDHelper.newUUID(),
            )
        )
    }

    private suspend fun updateAccount(
        current: CaldavAccount,
        url: String?,
        name: String,
        username: String,
        newSession: String?,
    ) {
        caldavDao.update(
            current.copy(
                name = name,
                url = url,
                username = username,
                password = newSession?.let { encryption.encrypt(it) } ?: current.password,
                error = "",
            )
        )
    }

    private suspend fun handleError(e: Exception) {
        Logger.e(e) { "Etebase account operation failed" }
        _state.update {
            it.copy(
                snackbar = when (e) {
                    is UnauthorizedException -> getString(Res.string.invalid_username_or_password)
                    is ConnectionException -> getString(Res.string.network_error)
                    is ConnectException -> getString(Res.string.network_error)
                    else -> getString(Res.string.error_adding_account, e.message ?: "")
                }
            )
        }
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.account?.let {
                try {
                    clientProvider.forAccount(it).logout()
                } catch (e: Exception) {
                    Logger.e(e) { "Etebase logout failed" }
                }
                taskDeleter.delete(it)
            }
            onComplete()
        }
    }
}
