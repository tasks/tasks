package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.tasks.auth.SignInHandler
import org.tasks.compose.accounts.Platform
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.gtasks_GPr_header

@Composable
fun GoogleTasksAccountSettingsDetail(
    pane: GoogleTasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val signInHandler = koinInject<SignInHandler>()
    val syncAdapters = koinInject<SyncAdapters>()
    val scope = rememberCoroutineScope()
    AccountSettingsDetail(
        account = pane.account,
        defaultName = Res.string.gtasks_GPr_header,
        onNavigateBack = onNavigateBack,
        onSignIn = {
            scope.launch {
                try {
                    signInHandler.signIn(Platform.GOOGLE_TASKS)
                    syncAdapters.sync(SyncSource.USER_INITIATED)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(e) { "Google Tasks re-authentication failed" }
                }
            }
        },
    )
}
