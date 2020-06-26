/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth

import android.accounts.AccountManager
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import com.todoroo.andlib.utility.DialogUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskListDao
import org.tasks.dialogs.DialogBuilder
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.play.AuthResultHandler
import org.tasks.preferences.ActivityPermissionRequestor
import org.tasks.preferences.PermissionRequestor
import javax.inject.Inject

/**
 * This activity allows users to sign in or log in to Google Tasks through the Android account
 * manager
 *
 * @author Sam Bosley
 */
@AndroidEntryPoint
class GtasksLoginActivity : InjectingAppCompatActivity() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var googleAccountManager: GoogleAccountManager
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var permissionRequestor: ActivityPermissionRequestor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permissionRequestor.requestAccountPermissions()) {
            chooseAccount()
        }
    }

    private fun chooseAccount() {
        val chooseAccountIntent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), false, null, null, null, null)
        startActivityForResult(chooseAccountIntent, RC_CHOOSE_ACCOUNT)
    }

    private fun getAuthToken(account: String) {
        val pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating)
        pd.show()
        getAuthToken(account, pd)
    }

    private fun getAuthToken(a: String, pd: ProgressDialog) {
        googleAccountManager.getTasksAuthToken(
                this,
                a,
                object : AuthResultHandler {
                    override fun authenticationSuccessful(accountName: String) {
                        var account = googleTaskListDao.getAccount(accountName)
                        if (account == null) {
                            account = GoogleTaskAccount()
                            account.account = accountName
                            googleTaskListDao.insert(account)
                        } else {
                            account.error = ""
                            googleTaskListDao.update(account)
                            googleTaskListDao.resetLastSync(accountName)
                        }
                        setResult(Activity.RESULT_OK)
                        DialogUtilities.dismissDialog(this@GtasksLoginActivity, pd)
                        finish()
                    }

                    override fun authenticationFailed(message: String) {
                        setResult(Activity.RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, message))
                        DialogUtilities.dismissDialog(this@GtasksLoginActivity, pd)
                        finish()
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_CHOOSE_ACCOUNT) {
            if (resultCode == Activity.RESULT_OK) {
                val account = data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!
                getAuthToken(account)
            } else {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PermissionRequestor.REQUEST_GOOGLE_ACCOUNTS) {
            if (verifyPermissions(grantResults)) {
                chooseAccount()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
        private const val RC_CHOOSE_ACCOUNT = 10988
    }
}