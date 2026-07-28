package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.gtasks_GPr_header

@Composable
fun GoogleTasksAccountSettingsDetail(
    pane: GoogleTasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    AccountSettingsDetail(
        account = pane.account,
        defaultName = Res.string.gtasks_GPr_header,
        onNavigateBack = onNavigateBack,
    )
}
