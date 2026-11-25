package org.tasks.googleapis

import android.content.Context
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.drive.DriveInvoker
import org.tasks.gtasks.AccountManagerCredentials
import org.tasks.preferences.Preferences
import javax.inject.Inject

class InvokerFactory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val credentials: AccountManagerCredentials,
        private val preferences: Preferences,
) {

    fun getDriveInvoker() = DriveInvoker(
            context,
            credentials.adapter(
                    preferences.getStringValue(R.string.p_google_drive_backup_account) ?: "",
                    DriveScopes.DRIVE_FILE,
            ),
    )

    fun getGtasksInvoker(account: String) = GtasksInvoker(
            credentials.adapter(account, TasksScopes.TASKS),
    )
}
