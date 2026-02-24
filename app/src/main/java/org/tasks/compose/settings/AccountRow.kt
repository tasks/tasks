package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.prefIcon
import org.tasks.data.prefTitle

@Composable
fun AccountRow(
    account: CaldavAccount,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val iconTint = if (account.isCaldavAccount || account.isLocalList) {
        colorResource(R.color.icon_tint_with_alpha)
    } else {
        Color.Unspecified
    }

    PreferenceRow(
        title = stringResource(account.prefTitle),
        iconRes = account.prefIcon,
        iconTint = iconTint,
        summary = account.name,
        showError = account.hasError,
        onClick = onClick,
        modifier = modifier
    )
}
