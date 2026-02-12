package org.tasks.preferences.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountResponse
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.TasksPreferences.Companion.cachedAccountData
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TasksAccountViewModel @Inject constructor(
        private val provider: CaldavClientProvider,
        private val firebase: Firebase,
        private val tasksPreferences: TasksPreferences,
) : ViewModel() {
    val newPassword = MutableStateFlow<NewPassword?>(null)

    private val json = Json { ignoreUnknownKeys = true }

    private val initialResponse: TasksAccountResponse? = runBlocking {
        tasksPreferences.get(cachedAccountData, "").takeIf(String::isNotBlank)?.let {
            try { json.decodeFromString<TasksAccountResponse>(it) }
            catch (e: Exception) { Timber.e(e); null }
        }
    }

    private val accountResponse: StateFlow<TasksAccountResponse?> =
        tasksPreferences.flow(cachedAccountData, "")
            .map { raw ->
                if (raw.isBlank()) return@map null
                try {
                    json.decodeFromString<TasksAccountResponse>(raw)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialResponse)

    val appPasswords: StateFlow<List<TasksAccountResponse.AppPassword>?> = accountResponse
        .map { it?.appPasswords }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inboundEmail: StateFlow<String?> = accountResponse
        .map { it?.inboundEmail?.email?.takeIf(String::isNotEmpty) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inboundCalendar: StateFlow<String?> = accountResponse
        .map { it?.inboundEmail?.calendar?.takeIf(String::isNotEmpty) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var inFlight = false

    private suspend fun refreshAccountData(account: CaldavAccount) {
        try {
            val response = provider.forTasksAccount(account).getAccount() ?: return
            tasksPreferences.set(cachedAccountData, response)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun refreshAccount(account: CaldavAccount) = viewModelScope.launch {
        refreshAccountData(account)
    }

    fun requestNewPassword(account: CaldavAccount, description: String) = viewModelScope.launch {
        if (inFlight) {
            return@launch
        }
        inFlight = true
        try {
            provider
                    .forTasksAccount(account)
                    .generateNewPassword(description.takeIf { it.isNotBlank() })
                    ?.let {
                        newPassword.value =
                                NewPassword(
                                        username = it.getString("username"),
                                        password = it.getString("password"),
                                )
                    }
        } catch (e: Exception) {
            Timber.e(e)
        }
        inFlight = false
    }

    fun deletePassword(account: CaldavAccount, id: Int) = viewModelScope.launch {
        try {
            provider.forTasksAccount(account).deletePassword(id)
            refreshAccountData(account)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun clearNewPassword() {
        newPassword.value = null
    }

    data class NewPassword(
        val username: String,
        val password: String,
    )

    fun regenerateInboundEmail(account: CaldavAccount) = viewModelScope.launch {
        try {
            provider.forTasksAccount(account).regenerateInboundEmail()
            refreshAccountData(account)
            firebase.logEvent(
                R.string.event_settings_click,
                R.string.param_type to "email_to_task_regenerate"
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun setInboundCalendar(account: CaldavAccount, calendar: String?) = viewModelScope.launch {
        try {
            provider.forTasksAccount(account).setInboundCalendar(calendar)
            refreshAccountData(account)
            firebase.logEvent(
                R.string.event_settings_click,
                R.string.param_type to "email_to_task_set_calendar"
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
