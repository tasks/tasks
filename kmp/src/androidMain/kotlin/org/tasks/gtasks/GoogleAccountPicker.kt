package org.tasks.gtasks

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.api.services.tasks.TasksScopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.google_tasks_permission_not_granted
import java.util.concurrent.atomic.AtomicLong

class GoogleAccountPicker(
    private val context: Context,
    private val googleAccountManager: GoogleAccountManager,
) {
    private val mutex = Mutex()

    suspend fun pick(
        loginHint: String?,
        scope: String = TasksScopes.TASKS,
        onAccountChosen: () -> Unit = {},
    ): String = mutex.withLock {
        val accountName = googleAccountManager.getAccount(loginHint)?.name
            ?: launch(chooseAccountIntent())
                .takeIf { it.resultCode == Activity.RESULT_OK }
                ?.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            ?: throw CancellationException("No account chosen")
        onAccountChosen()
        googleAccountManager.authorize(accountName, scope)?.let { consent ->
            launch(consent)
            if (googleAccountManager.authorize(accountName, scope) != null) {
                if (scope == TasksScopes.TASKS) {
                    throw DisplayableException(Res.string.google_tasks_permission_not_granted)
                }
                throw CancellationException("Access not granted")
            }
        }
        accountName
    }

    private suspend fun launch(intent: Intent): ActivityResult {
        val request = Request(nextId.incrementAndGet(), intent)
        pending = request
        try {
            GoogleAccountPickerActivity.start(context, request.id)
            return request.result.await()
        } finally {
            request.result.cancel()
            if (pending === request) {
                pending = null
            }
        }
    }

    private fun chooseAccountIntent() = AccountManager.newChooseAccountIntent(
        null, null, arrayOf(GoogleAccountManager.ACCOUNT_TYPE), null, null, null, null
    )

    internal class Request(val id: Long, val intent: Intent) {
        val result = CompletableDeferred<ActivityResult>()
    }

    internal companion object {
        private val nextId = AtomicLong()

        @Volatile
        var pending: Request? = null
    }
}
