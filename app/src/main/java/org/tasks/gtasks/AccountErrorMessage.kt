package org.tasks.gtasks

import android.content.Context
import org.tasks.R
import org.tasks.resources.getString
import org.tasks.ui.DisplayableException

suspend fun Context.accountErrorMessage(e: Exception): String? = when (e) {
    is GoogleAccountManager.AccountNotFoundException ->
        getString(R.string.gtasks_error_accountNotFound, e.accountName)
    is DisplayableException -> getString(e.resource)
    else -> e.message
}
