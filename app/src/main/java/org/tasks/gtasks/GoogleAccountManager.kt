package org.tasks.gtasks

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import com.google.common.collect.Lists
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.play.AuthResultHandler
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class GoogleAccountManager @Inject constructor(
        @ApplicationContext context: Context?,
        private val permissionChecker: PermissionChecker,
        private val preferences: Preferences
) {
    private val accountManager: AccountManager = AccountManager.get(context)

    val accounts: List<String>
        get() = Lists.transform(accountList) { account: Account? -> account!!.name }

    private val accountList: List<Account>
        get() = if (permissionChecker.canAccessAccounts()) {
            accountManager.getAccountsByType("com.google").toList()
        } else {
            emptyList()
        }

    fun getAccount(name: String): Account? = if (isNullOrEmpty(name)) {
        null
    } else {
        accountList.find { name.equals(it.name, ignoreCase = true) }
    }

    fun canAccessAccount(name: String): Boolean {
        return getAccount(name) != null
    }

    fun getAccessToken(name: String, scope: String): String? {
        AndroidUtilities.assertNotMainThread()
        val account = getAccount(name)
        if (account == null) {
            Timber.e("Cannot find account %s", name)
            return null
        }
        val alreadyNotified = preferences.alreadyNotified(name, scope)
        return try {
            val token = accountManager.blockingGetAuthToken(account, "oauth2:$scope", !alreadyNotified)
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

    fun getTasksAuthToken(activity: Activity, accountName: String, handler: AuthResultHandler) {
        getToken(TasksScopes.TASKS, activity, accountName, handler)
    }

    fun getDriveAuthToken(activity: Activity, accountName: String, handler: AuthResultHandler) {
        getToken(DriveScopes.DRIVE_FILE, activity, accountName, handler)
    }

    @SuppressLint("CheckResult")
    private fun getToken(
            scope: String, activity: Activity, accountName: String, handler: AuthResultHandler) {
        val account = getAccount(accountName)
        Single.fromCallable {
            if (account == null) {
                throw RuntimeException(
                        activity.getString(R.string.gtasks_error_accountNotFound, accountName))
            }
            AndroidUtilities.assertNotMainThread()
            accountManager
                    .getAuthToken(account, "oauth2:$scope", Bundle(), activity, null, null)
                    .result
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { bundle: Bundle ->
                            preferences.setAlreadyNotified(accountName, scope, false)
                            val intent = bundle[AccountManager.KEY_INTENT] as Intent?
                            if (intent != null) {
                                activity.startActivity(intent)
                            } else {
                                handler.authenticationSuccessful(accountName)
                            }
                        }
                ) { e: Throwable ->
                    Timber.e(e)
                    handler.authenticationFailed(e.message)
                }
    }

    fun invalidateToken(token: String?) {
        accountManager.invalidateAuthToken("com.google", token)
    }
}