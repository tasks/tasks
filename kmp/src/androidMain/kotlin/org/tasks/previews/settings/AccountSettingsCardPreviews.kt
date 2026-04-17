package org.tasks.previews.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.tasks.compose.settings.AccountSettingsCard
import org.tasks.compose.settings.ManageSubscriptionSheetContent
import org.tasks.compose.settings.ProCardState
import org.tasks.data.entity.CaldavAccount
import org.tasks.themes.TasksTheme

@PreviewLightDark
@Composable
private fun UpgradePreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.Upgrade,
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SubscribedMonthlyPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.Subscribed(
                isMonthly = true,
                formattedPrice = "$2.99",
            ),
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SubscribedAnnualPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.Subscribed(
                isMonthly = false,
                formattedPrice = "$29.99",
            ),
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SignInPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.SignIn,
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TasksOrgAccountPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.TasksOrgAccount(
                account = CaldavAccount(
                    name = "user@tasks.org",
                    accountType = CaldavAccount.TYPE_TASKS,
                ),
            ),
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TasksOrgAccountLoadingPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.TasksOrgAccount(
                account = CaldavAccount(
                    name = "user@tasks.org",
                    accountType = CaldavAccount.TYPE_TASKS,
                ),
                isLoading = true,
            ),
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TasksOrgAccountErrorPreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.TasksOrgAccount(
                account = CaldavAccount(
                    name = "user@tasks.org",
                    accountType = CaldavAccount.TYPE_TASKS,
                    error = "Authentication failed",
                ),
            ),
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DonatePreview() {
    TasksTheme {
        AccountSettingsCard(
            state = ProCardState.Donate,
            onClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ManageSubscriptionSheetPreview() {
    TasksTheme {
        ManageSubscriptionSheetContent(
            onUpgrade = {},
            onModify = {},
            onCancel = {},
        )
    }
}
