package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.themes.TasksTheme

private val ProGoldColor = Color(0xFFFFB300)

data class AccountData(
    val createdAt: Long? = null,
    val subscriptionFree: Boolean = true,
    val subscriptionProvider: String? = null,
    val subscriptionExpiration: Long? = null,
)

sealed class ProCardState {
    data object Upgrade : ProCardState()
    data class Subscribed(
        val isMonthly: Boolean,
        val subscriptionPrice: Int,
    ) : ProCardState()
    data object SignIn : ProCardState()
    data class TasksOrgAccount(
        val account: CaldavAccount,
        val isLoading: Boolean = false,
        val accountData: AccountData? = null,
    ) : ProCardState()
    data object Donate : ProCardState()
}

@Composable
fun AccountSettingsCard(
    state: ProCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOverride: ImageVector? = null,
    showChevron: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsCardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = SettingsRowPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val defaultTint = colorResource(R.color.icon_tint_with_alpha)
            val errorColor = colorResource(R.color.overdue)

            var iconRes: Int?
            val iconTint: Color
            val title: String
            val summary: String?
            val showError: Boolean

            when (state) {
                is ProCardState.Upgrade -> {
                    iconRes = null
                    iconTint = ProGoldColor
                    title = stringResource(R.string.upgrade_to_pro)
                    summary = stringResource(R.string.upgrade_to_pro_card_description)
                    showError = false
                }
                is ProCardState.Subscribed -> {
                    iconRes = null
                    iconTint = ProGoldColor
                    title = stringResource(R.string.supporter)
                    val interval = if (state.isMonthly) {
                        R.string.price_per_month
                    } else {
                        R.string.price_per_year
                    }
                    val price = (state.subscriptionPrice - .01).toString()
                    summary = stringResource(
                        R.string.supporter_summary,
                        stringResource(interval, price)
                    )
                    showError = false
                }
                is ProCardState.SignIn -> {
                    iconRes = R.drawable.ic_round_icon
                    iconTint = Color.Unspecified
                    title = stringResource(R.string.tasks_org)
                    summary = stringResource(R.string.sign_in_to_connect)
                    showError = false
                }
                is ProCardState.TasksOrgAccount -> {
                    iconRes = R.drawable.ic_round_icon
                    iconTint = Color.Unspecified
                    title = stringResource(R.string.tasks_org)
                    summary = state.account.name
                    showError = state.account.hasError
                }
                is ProCardState.Donate -> {
                    iconRes = R.drawable.ic_outline_favorite_border_24px
                    iconTint = defaultTint
                    title = stringResource(R.string.donate)
                    summary = stringResource(R.string.donate_nag)
                    showError = false
                }
            }

            val iconModifier = Modifier
                .padding(start = SettingsContentPadding)
                .size(SettingsIconSize)
            if (iconOverride != null) {
                Icon(
                    imageVector = iconOverride,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes!!),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(SettingsContentPadding))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (state is ProCardState.TasksOrgAccount && state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    strokeWidth = 2.dp,
                )
            } else if (showError) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_error_outline_24px),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = errorColor
                )
            } else if (showChevron) {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right_24px),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = defaultTint
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize)
                )
            }
        }
    }
}

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
                subscriptionPrice = 3,
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
                subscriptionPrice = 30,
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

@Composable
fun ManageSubscriptionSheetContent(
    onUpgrade: () -> Unit,
    onModify: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    showUpgrade: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsContentPadding)
            .padding(bottom = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showUpgrade) {
            AccountSettingsCard(
                state = ProCardState.Upgrade,
                onClick = onUpgrade,
                iconOverride = Icons.Outlined.RocketLaunch,
                showChevron = false,
            )
        }
        SettingsItemCard {
            PreferenceRow(
                title = stringResource(R.string.manage_subscription),
                icon = Icons.Outlined.Edit,
                iconTint = colorResource(R.color.icon_tint_with_alpha),
                onClick = onModify,
            )
        }
        DangerCard(
            icon = Icons.Outlined.RemoveCircleOutline,
            title = stringResource(R.string.button_unsubscribe),
            tint = colorResource(R.color.overdue),
            trailingIcon = R.drawable.ic_open_in_new_24px,
            onClick = onCancel,
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
