package org.tasks.gtasks

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class GoogleAccountManager @Inject constructor(
        @ApplicationContext context: Context?,
        private val preferences: Preferences
) {
    private val accountManager: AccountManager = AccountManager.get(context)

    val accounts: List<String>
        get() = accountList.map { it.name }

    private val accountList: List<Account>
        get() = accountManager.getAccountsByType("com.google").toList()

    fun getAccount(name: String?): Account? = if (isNullOrEmpty(name)) {
        null
    } else {
        accountList.find { name.equals(it.name, ignoreCase = true) }
    }

    fun canAccessAccount(name: String): Boolean = getAccount(name) != null

    suspend fun getAccessToken(name: String?, scope: String): String? {
        val account = name?.let { getAccount(it) }
        if (account == null) {
            Timber.e("Cannot find account %s", name)
            return null
        }
        val alreadyNotified = preferences.alreadyNotified(name, scope)
        return try {
            val token = withContext(Dispatchers.IO) {
                accountManager.blockingGetAuthToken(account, "oauth2:$scope", !alreadyNotified)
            }
            preferences.setAlreadyNotified(name, scope, isNullOrEmpty(token))
            token
        } catch (e: AuthenticatorException) {
            Timber.e(e)
            null
        } catch (e: IOException) {
            Timber.e(e)
            null
        } catch (e: OperationCanceledException) {
            Timber.e(e)
            null
        }
    }

    suspend fun getTasksAuthToken(activity: Activity, accountName: String): Bundle? =
            getToken(TasksScopes.TASKS, activity, accountName)

    suspend fun getDriveAuthToken(activity: Activity, accountName: String): Bundle? =
            getToken(DriveScopes.DRIVE_FILE, activity, accountName)

    @SuppressLint("CheckResult")
    private suspend fun getToken(scope: String, activity: Activity, accountName: String): Bundle? {
        val account = getAccount(accountName)
                ?: throw RuntimeException(
                        activity.getString(R.string.gtasks_error_accountNotFound, accountName))
        return withContext(Dispatchers.IO) {
            val bundle = accountManager
                    .getAuthToken(account, "oauth2:$scope", Bundle(), activity, null, null)
                    .result
            preferences.setAlreadyNotified(accountName, scope, false)
            bundle
        }
    }

    fun invalidateToken(token: String?) {
        accountManager.invalidateAuthToken("com.google", token)
    }
}