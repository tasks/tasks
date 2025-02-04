package org.tasks.googleapis

import android.content.Context
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import com.todoroo.astrid.gtasks.api.HttpCredentialsAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.drive.DriveInvoker
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.preferences.Preferences
import javax.inject.Inject

class InvokerFactory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val googleAccountManager: GoogleAccountManager,
        private val preferences: Preferences,
) {

    fun getDriveInvoker() = DriveInvoker(
            context,
            HttpCredentialsAdapter(
                    googleAccountManager,
                    preferences.getStringValue(R.string.p_google_drive_backup_account) ?: "",
                    DriveScopes.DRIVE_FILE
            ),
    )

    fun getGtasksInvoker(account: String) = GtasksInvoker(
            HttpCredentialsAdapter(googleAccountManager, account, TasksScopes.TASKS),
    )
}