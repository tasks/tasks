package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.prefIcon
import org.tasks.data.prefTitle
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.tasks_org

@Composable
fun AccountRow(
    account: CaldavAccount,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val iconTint = if (account.isCaldavAccount || account.isLocalList) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.Unspecified
    }

    PreferenceRow(
        title = if (account.isTasksOrg) {
            org.jetbrains.compose.resources.stringResource(Res.string.tasks_org)
        } else {
            stringResource(account.prefTitle)
        },
        iconRes = account.prefIcon,
        iconTint = iconTint,
        summary = account.name,
        showError = account.hasError,
        onClick = onClick,
        modifier = modifier
    )
}
