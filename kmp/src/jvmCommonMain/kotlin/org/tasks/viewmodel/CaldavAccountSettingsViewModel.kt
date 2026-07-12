package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.dav4jvm.okhttp.exception.HttpException
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.metadata.TagMetadataSync
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
import tasks.kmp.generated.resources.metadata_not_supported
import tasks.kmp.generated.resources.metadata_stored_on_account
import tasks.kmp.generated.resources.metadata_stored_on_tasks_org
import tasks.kmp.generated.resources.network_error
import tasks.kmp.generated.resources.password_required
import tasks.kmp.generated.resources.sync_metadata_summary
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
    private val tagMetadataSync: TagMetadataSync,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(CaldavAccountState())
    val state: StateFlow<CaldavAccountState> = _state

    private var metadataProbeSeq = 0

    init {
        viewModelScope.launch {
            accountId
                .filterNotNull()
                .flatMapLatest { id ->
                    combine(caldavDao.watchAccount(id), caldavDao.watchAccounts()) { account, _ -> account }
                }
                .collect { account ->
                    _state.update { it.copy(account = account) }
                    refreshMetadataCatching()
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
        viewModelScope.launch { refreshMetadataCatching() }
    }

    private suspend fun refreshMetadataCatching() {
        try {
            refreshMetadata()
        } catch (e: Exception) {
            Logger.e(e) { "refreshMetadata failed" }
        }
    }

    private suspend fun refreshMetadata() {
        val account = _state.value.account
        val toggle = if (account != null) {
            tagMetadataSync.toggleState(account)
        } else {
            tagMetadataSync.newAccountToggleState()
        }
        if (!toggle.visible) {
            _state.update { it.copy(metadataVisible = false) }
            return
        }
        val subtitle = when {
            toggle.forcedByTasksOrg -> getString(Res.string.metadata_stored_on_tasks_org)
            toggle.otherPrimary != null ->
                getString(Res.string.metadata_stored_on_account, toggle.otherPrimary)
            else -> getString(Res.string.sync_metadata_summary)
        }
        _state.update {
            it.copy(
                metadataVisible = true,
                metadataChecked = if (account != null) toggle.checked else it.metadataChecked,
                metadataInteractable = toggle.interactable,
                metadataSubtitle = subtitle,
            )
        }
    }

    fun watchNewAccountMetadata() {
        if (accountId.value != null) return
        viewModelScope.launch {
            caldavDao.watchAccounts().collect {
                if (_state.value.account == null) refreshMetadataCatching()
            }
        }
    }

    fun onMetadataToggle(enable: Boolean) {
        val account = _state.value.account
        if (account == null) {
            onNewAccountMetadataToggle(enable)
            return
        }
        if (_state.value.metadataProbing) return
        _state.update { it.copy(metadataProbing = true) }
        if (!enable) {
            disableMetadata()
            return
        }
        viewModelScope.launch {
            val current = try {
                tagMetadataSync.primaryAccount()
            } catch (e: Exception) {
                Logger.e(e) { "primaryAccount lookup failed" }
                _state.update { it.copy(metadataProbing = false) }
                return@launch
            }
            if (current != null && current.id != account.id) {
                _state.update {
                    it.copy(metadataProbing = false, metadataSwitchFrom = current.name ?: current.username ?: "")
                }
            } else {
                enableMetadata(account)
            }
        }
    }

    private fun onNewAccountMetadataToggle(enable: Boolean) {
        val seq = ++metadataProbeSeq
        if (!enable) {
            _state.update { it.copy(metadataChecked = false, metadataProbing = false) }
            return
        }
        _state.update { it.copy(metadataProbing = true) }
        viewModelScope.launch {
            val viable = try {
                if (!validate()) {
                    if (seq == metadataProbeSeq) _state.update { it.copy(metadataProbing = false) }
                    return@launch
                }
                val s = _state.value
                val username = s.username.trim()
                val password = s.password
                val homeSet = withContext(Dispatchers.IO) {
                    caldavClientProvider.forUrl(s.url.trim(), username, password).homeSet(username, password)
                }
                tagMetadataSync.probeViability(homeSet, username, password)
            } catch (e: Exception) {
                Logger.e(e) { "metadata probe failed" }
                if (seq == metadataProbeSeq) {
                    _state.update { it.copy(metadataProbing = false) }
                    handleError(e)
                }
                return@launch
            }
            if (seq != metadataProbeSeq) return@launch
            _state.update {
                it.copy(
                    metadataChecked = viable,
                    metadataProbing = false,
                    snackbar = if (viable) it.snackbar else getString(Res.string.metadata_not_supported),
                )
            }
        }
    }

    fun confirmMetadataSwitch() {
        _state.update { it.copy(metadataSwitchFrom = null) }
        _state.value.account?.let { enableMetadata(it) }
    }

    fun cancelMetadataSwitch() = _state.update { it.copy(metadataSwitchFrom = null) }

    private fun enableMetadata(account: CaldavAccount) = viewModelScope.launch {
        _state.update { it.copy(metadataProbing = true) }
        runEnablePrimary(account)
        _state.update { it.copy(metadataProbing = false) }
    }

    private suspend fun runEnablePrimary(account: CaldavAccount): Boolean {
        val supported = try {
            withContext(NonCancellable) {
                tagMetadataSync.enablePrimary(account)
            }
        } catch (e: Exception) {
            Logger.e(e) { "metadata probe failed" }
            _state.update { it.copy(snackbar = getString(Res.string.network_error)) }
            refreshMetadataCatching()
            return false
        }
        if (!supported) {
            _state.update { it.copy(snackbar = getString(Res.string.metadata_not_supported)) }
        }
        refreshMetadataCatching()
        return supported
    }

    private fun disableMetadata() = viewModelScope.launch {
        try {
            withContext(NonCancellable) { tagMetadataSync.disable() }
        } catch (e: Exception) {
            Logger.e(e) { "metadata disable failed" }
        }
        _state.update { it.copy(metadataProbing = false) }
        refreshMetadataCatching()
    }

    fun setUrl(value: String) {
        resetPendingMetadataProbe()
        _state.update { it.copy(url = value, urlError = null) }
    }

    fun setUsername(value: String) {
        resetPendingMetadataProbe()
        _state.update { it.copy(username = value, usernameError = null) }
    }

    fun setPassword(value: String) {
        resetPendingMetadataProbe()
        _state.update { it.copy(password = value, passwordError = null) }
    }

    private fun resetPendingMetadataProbe() {
        val s = _state.value
        if (s.account != null || (!s.metadataChecked && !s.metadataProbing)) return
        metadataProbeSeq++
        _state.update { it.copy(metadataChecked = false, metadataProbing = false) }
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
                val account = addAccount(principal, effectiveName, usernameValue, passwordValue, s.serverType)
                reporting.logEvent(
                    "sync_add_account",
                    "type" to Constants.SYNC_TYPE_CALDAV,
                )
                if (s.metadataChecked) {
                    val enabled = withContext(NonCancellable) {
                        tagMetadataSync.enablePrimary(account, skipProbe = true)
                    }
                    if (!enabled) Logger.w { "metadata enable did not complete after a passing probe" }
                }
                backgroundWork.sync(SyncSource.ACCOUNT_ADDED)
                onComplete()
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
    ): CaldavAccount {
        val account = CaldavAccount(
            accountType = CaldavAccount.TYPE_CALDAV,
            name = name,
            url = principal,
            username = username,
            password = encryption.encrypt(password),
            uuid = UUIDHelper.newUUID(),
            serverType = serverType,
        )
        return account.copy(id = caldavDao.insert(account))
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
