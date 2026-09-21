package org.tasks.compose.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.tasks.BuildConfig
import org.tasks.data.entity.CaldavAccount

@Composable
fun AndroidMainSettingsScreen(
    accounts: List<CaldavAccount>,
    proCardState: ProCardState?,
    environmentLabel: String? = null,
    showBackupWarning: Boolean,
    showWidgets: Boolean,
    isDebug: Boolean = BuildConfig.DEBUG,
    onAccountClick: (CaldavAccount) -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: (SettingsDestination) -> Unit,
    onProCardClick: () -> Unit,
    showDesktopLinking: Boolean = false,
    onLinkDesktopClick: () -> Unit = {},
) {
    MainSettingsScreen(
        accounts = accounts,
        proCardState = proCardState,
        environmentLabel = environmentLabel,
        showBackupWarning = showBackupWarning,
        showWidgets = showWidgets,
        isDebug = isDebug,
        onAccountClick = onAccountClick,
        onAddAccountClick = onAddAccountClick,
        onSettingsClick = onSettingsClick,
        onProCardClick = onProCardClick,
        showDesktopLinking = showDesktopLinking,
        onLinkDesktopClick = onLinkDesktopClick,
        bottomContent = {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        },
    )
}
