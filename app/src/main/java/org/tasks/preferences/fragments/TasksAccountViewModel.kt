package org.tasks.preferences.fragments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.entity.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TasksAccountViewModel @Inject constructor(
        private val provider: CaldavClientProvider
) : ViewModel() {
    val newPassword = MutableLiveData<AppPassword?>()
    val appPasswords = MutableLiveData<List<AppPassword>?>()

    private var inFlight = false

    fun refreshPasswords(account: CaldavAccount) = viewModelScope.launch {
        try {
            provider
                    .forTasksAccount(account)
                    .getAppPasswords()
                    ?.let {
                        val passwords = it.getJSONArray(PASSWORDS)
                        val result = ArrayList<AppPassword>()
                        for (i in 0 until passwords.length()) {
                            with(passwords.getJSONObject(i)) {
                                result.add(AppPassword(
                                        description = getStringOrNull(DESCRIPTION),
                                        id = getInt(SESSION_ID),
                                        createdAt = getLongOrNull(CREATED_AT),
                                        lastAccess = getLongOrNull(LAST_ACCESS)
                                ))
                            }
                        }
                        appPasswords.value = result
                    }
        } catch (e: Exception) {
            Timber.e(e)
        }
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
                                AppPassword(
                                        username = it.getString(USERNAME),
                                        password = it.getString(PASSWORD)
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
            refreshPasswords(account)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun clearNewPassword() {
        newPassword.value = null
    }

    data class AppPassword(
            val username: String? = null,
            val password: String? = null,
            val description: String? = null,
            val id: Int = -1,
            val createdAt: Long? = null,
            val lastAccess: Long? = null
    )

    companion object {
        private const val PASSWORDS = "passwords"
        private const val DESCRIPTION = "description"
        private const val SESSION_ID = "session_id"
        private const val CREATED_AT = "created_at"
        private const val LAST_ACCESS = "last_access"
        private const val PASSWORD = "password"
        private const val USERNAME = "username"

        fun JSONObject.getStringOrNull(key: String) = if (isNull(key)) null else getString(key)

        fun JSONObject.getLongOrNull(key: String) = if (isNull(key)) null else getLong(key)
    }
}