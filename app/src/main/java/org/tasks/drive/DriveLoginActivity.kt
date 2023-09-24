/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.drive

import android.accounts.AccountManager
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DialogUtilities
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.preferences.Preferences
import javax.inject.Inject

/**
 * This activity allows users to sign in or log in to Google Tasks through the Android account
 * manager
 *
 * @author Sam Bosley
 */
@AndroidEntryPoint
class DriveLoginActivity : AppCompatActivity() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var googleAccountManager: GoogleAccountManager
    @Inject lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chooseAccountIntent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), null, null, null, null)
        startActivityForResult(chooseAccountIntent, RC_CHOOSE_ACCOUNT)
    }

    private suspend fun getAuthToken(account: String?) {
        val pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating)
        pd.show()
        getAuthToken(account, pd)
    }

    private suspend fun getAuthToken(accountName: String?, pd: ProgressDialog) {
        try {
            googleAccountManager.getDriveAuthToken(this, accountName!!)
                    ?.let { bundle ->
                        val intent = bundle[AccountManager.KEY_INTENT]
                        if (intent is Intent) {
                            startActivity(intent)
                        } else {
                            preferences.setString(R.string.p_google_drive_backup_account, accountName)
                            preferences.setBoolean(R.string.p_google_drive_backup, true)
                            setResult(RESULT_OK)
                            DialogUtilities.dismissDialog(this@DriveLoginActivity, pd)
                            finish()
                        }
                    }
        } catch (e: Exception) {
            preferences.setBoolean(R.string.p_google_drive_backup, false)
            setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, e.message))
            DialogUtilities.dismissDialog(this@DriveLoginActivity, pd)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_CHOOSE_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                val account = data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                lifecycleScope.launch {
                    getAuthToken(account)
                }
            } else {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
        private const val RC_CHOOSE_ACCOUNT = 10988
    }
}