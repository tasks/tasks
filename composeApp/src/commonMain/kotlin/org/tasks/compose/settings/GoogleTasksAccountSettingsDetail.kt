package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.tasks.compose.accounts.Platform
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.gtasks_GPr_header

@Composable
fun GoogleTasksAccountSettingsDetail(
    pane: GoogleTasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    AccountSettingsDetail(
        accountId = pane.accountId,
        defaultName = Res.string.gtasks_GPr_header,
        onNavigateBack = onNavigateBack,
        platform = Platform.GOOGLE_TASKS,
        loginHint = { it.username },
    )
}
