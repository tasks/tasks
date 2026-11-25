package org.tasks.gtasks

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.BundleCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GoogleAccountManager(
    context: Context,
    private val authErrors: AuthErrorNotifications,
) {
    private val accountManager: AccountManager = AccountManager.get(context)

    val accounts: List<String>
        get() = accountList.map { it.name }

    private val accountList: List<Account>
        get() = accountManager.getAccountsByType(ACCOUNT_TYPE).toList()

    fun getAccount(name: String?): Account? = if (name.isNullOrEmpty()) {
        null
    } else {
        accountList.find { name.equals(it.name, ignoreCase = true) }
    }

    fun canAccessAccount(name: String): Boolean = getAccount(name) != null

    @Throws(IOException::class)
    suspend fun getAccessToken(name: String?, scope: String): String? {
        val account = getAccount(name)
        if (name == null || account == null) {
            Logger.e(TAG) { "Cannot find account $name" }
            return null
        }
        val alreadyNotified = authErrors.alreadyNotified(name, scope)
        return try {
            val token = withContext(Dispatchers.IO) {
                accountManager.blockingGetAuthToken(account, "oauth2:$scope", !alreadyNotified)
            }
            authErrors.setAlreadyNotified(name, scope, token.isNullOrEmpty())
            token
        } catch (e: AuthenticatorException) {
            Logger.e(TAG, e) { "getAccessToken failed" }
            null
        } catch (e: IOException) {
            throw TokenUnavailableException(e)
        } catch (e: OperationCanceledException) {
            Logger.e(TAG, e) { "getAccessToken failed" }
            null
        }
    }

    suspend fun authorize(accountName: String, scope: String): Intent? {
        val account = getAccount(accountName) ?: throw AccountNotFoundException(accountName)
        val bundle = withContext(Dispatchers.IO) {
            accountManager
                .getAuthToken(account, "oauth2:$scope", Bundle(), false, null, null)
                .result
        }
        val consent = BundleCompat.getParcelable(bundle, AccountManager.KEY_INTENT, Intent::class.java)
        if (consent == null) {
            authErrors.setAlreadyNotified(accountName, scope, false)
        }
        return consent
    }

    fun invalidateToken(token: String?) {
        if (token != null) {
            accountManager.invalidateAuthToken(ACCOUNT_TYPE, token)
        }
    }

    interface AuthErrorNotifications {
        suspend fun alreadyNotified(account: String, scope: String): Boolean
        suspend fun setAlreadyNotified(account: String, scope: String, notified: Boolean)
    }

    class TokenUnavailableException(cause: IOException) : IOException(cause.message, cause)

    class AccountNotFoundException(val accountName: String) : Exception("Account $accountName not found")

    companion object {
        private const val TAG = "GoogleAccountManager"
        const val ACCOUNT_TYPE = "com.google"
    }
}
