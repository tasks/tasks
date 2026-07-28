package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.tasks.auth.SignInHandler
import org.tasks.compose.accounts.Platform
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.microsoft
import tasks.kmp.generated.resources.sign_in

@Composable
fun MicrosoftAccountSettingsDetail(
    pane: MicrosoftAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val signInHandler = koinInject<SignInHandler>()
    val scope = rememberCoroutineScope()
    AccountSettingsDetail(
        account = pane.account,
        defaultName = Res.string.microsoft,
        onNavigateBack = onNavigateBack,
        signInTitle = Res.string.sign_in,
        onSignIn = {
            scope.launch {
                try {
                    signInHandler.signIn(Platform.MICROSOFT)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(e) { "Microsoft re-authentication failed" }
                }
            }
        },
    )
}
