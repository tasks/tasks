/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DialogUtilities
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.auth.setupGoogleTasksAccount
import org.tasks.data.dao.CaldavDao
import org.tasks.dialogs.DialogBuilder
import org.tasks.gtasks.GoogleAccountPicker
import org.tasks.gtasks.accountErrorMessage
import javax.inject.Inject

@AndroidEntryPoint
class GtasksLoginActivity : AppCompatActivity() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var googleAccountPicker: GoogleAccountPicker
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var firebase: Firebase

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val accountName = googleAccountPicker.pick(
                    loginHint = null,
                    onAccountChosen = {
                        progressDialog = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating)
                            .apply { show() }
                    },
                )
                withContext(NonCancellable) {
                    setupGoogleTasksAccount(accountName, caldavDao, firebase)
                }
                setResult(RESULT_OK)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, accountErrorMessage(e)))
            } finally {
                progressDialog?.let { DialogUtilities.dismissDialog(this@GtasksLoginActivity, it) }
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
    }
}
