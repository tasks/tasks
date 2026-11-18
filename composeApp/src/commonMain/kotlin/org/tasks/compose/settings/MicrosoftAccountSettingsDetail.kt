package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.tasks.compose.accounts.Platform
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.microsoft
import tasks.kmp.generated.resources.sign_in

@Composable
fun MicrosoftAccountSettingsDetail(
    pane: MicrosoftAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    AccountSettingsDetail(
        accountId = pane.accountId,
        defaultName = Res.string.microsoft,
        onNavigateBack = onNavigateBack,
        platform = Platform.MICROSOFT,
        loginHint = { it.name },
        signInTitle = Res.string.sign_in,
    )
}
