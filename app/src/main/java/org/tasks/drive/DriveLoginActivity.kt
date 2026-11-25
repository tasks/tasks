package org.tasks.drive

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.api.services.drive.DriveScopes
import com.todoroo.andlib.utility.DialogUtilities
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.gtasks.GoogleAccountPicker
import org.tasks.gtasks.accountErrorMessage
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class DriveLoginActivity : AppCompatActivity() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var googleAccountPicker: GoogleAccountPicker
    @Inject lateinit var preferences: Preferences

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val accountName = googleAccountPicker.pick(
                    loginHint = null, scope = DriveScopes.DRIVE_FILE,
                    onAccountChosen = {
                        progressDialog = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating)
                            .apply { show() }
                    },
                )
                preferences.setString(R.string.p_google_drive_backup_account, accountName)
                preferences.setBoolean(R.string.p_google_drive_backup, true)
                setResult(RESULT_OK)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                preferences.setBoolean(R.string.p_google_drive_backup, false)
                setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, accountErrorMessage(e)))
            } finally {
                progressDialog?.let { DialogUtilities.dismissDialog(this@DriveLoginActivity, it) }
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
    }
}
