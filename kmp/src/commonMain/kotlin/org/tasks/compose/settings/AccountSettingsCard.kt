package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.button_unsubscribe
import tasks.kmp.generated.resources.donate
import tasks.kmp.generated.resources.donate_nag
import tasks.kmp.generated.resources.guest
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.manage_subscription
import tasks.kmp.generated.resources.price_per_month_with_currency
import tasks.kmp.generated.resources.price_per_year_with_currency
import tasks.kmp.generated.resources.sign_in_to_connect
import tasks.kmp.generated.resources.supporter
import tasks.kmp.generated.resources.supporter_summary
import tasks.kmp.generated.resources.supporter_summary_no_price
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.upgrade_to_pro
import tasks.kmp.generated.resources.upgrade_to_pro_card_description
import org.tasks.data.entity.CaldavAccount

private val ProGoldColor = Color(0xFFFFB300)

data class AccountData(
    val createdAt: Long? = null,
    val subscriptionFree: Boolean = true,
    val subscriptionProvider: String? = null,
    val subscriptionExpiration: Long? = null,
    val guest: Boolean = false,
)

sealed class ProCardState {
    data object Upgrade : ProCardState()
    data class Subscribed(
        val isMonthly: Boolean,
        val formattedPrice: String? = null,
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
    iconOverride: String? = null,
    showChevron: Boolean = true,
    environmentLabel: String? = null,
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
            val defaultTint = MaterialTheme.colorScheme.onSurfaceVariant
            val errorColor = MaterialTheme.colorScheme.error

            var iconVector: String? = null
            var useAppIcon = false
            val iconTint: Color
            val title: String
            val summary: String?
            val showError: Boolean

            when (state) {
                is ProCardState.Upgrade -> {
                    iconTint = ProGoldColor
                    title = stringResource(Res.string.upgrade_to_pro)
                    summary = stringResource(Res.string.upgrade_to_pro_card_description)
                    showError = false
                }
                is ProCardState.Subscribed -> {
                    iconTint = ProGoldColor
                    title = stringResource(Res.string.supporter)
                    summary = when {
                        state.formattedPrice == null -> null
                        state.formattedPrice.isBlank() ->
                            stringResource(Res.string.supporter_summary_no_price)
                        else -> {
                            val interval = if (state.isMonthly) {
                                Res.string.price_per_month_with_currency
                            } else {
                                Res.string.price_per_year_with_currency
                            }
                            stringResource(
                                Res.string.supporter_summary,
                                stringResource(interval, state.formattedPrice)
                            )
                        }
                    }
                    showError = false
                }
                is ProCardState.SignIn -> {
                    useAppIcon = true
                    iconTint = Color.Unspecified
                    val tasksOrgName = stringResource(Res.string.tasks_org)
                    title = environmentLabel
                        ?.let { "$tasksOrgName \u2022 $it" }
                        ?: tasksOrgName
                    summary = stringResource(Res.string.sign_in_to_connect)
                    showError = false
                }
                is ProCardState.TasksOrgAccount -> {
                    useAppIcon = true
                    iconTint = Color.Unspecified
                    title = buildString {
                        append(stringResource(Res.string.tasks_org))
                        environmentLabel?.let { append(" \u2022 $it") }
                        if (state.accountData?.guest == true) {
                            append(" \u2022 ${stringResource(Res.string.guest)}")
                        }
                    }
                    summary = state.account.name
                    showError = state.account.hasError
                }
                is ProCardState.Donate -> {
                    iconVector = TasksIcons.FAVORITE_BORDER
                    iconTint = defaultTint
                    title = stringResource(Res.string.donate)
                    summary = stringResource(Res.string.donate_nag)
                    showError = false
                }
            }

            val iconModifier = Modifier
                .padding(start = SettingsContentPadding)
                .size(SettingsIconSize)
            if (iconOverride != null) {
                SymbolIcon(
                    name = iconOverride,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else if (iconVector != null) {
                SymbolIcon(
                    name = iconVector,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else if (useAppIcon) {
                Icon(
                    painter = painterResource(Res.drawable.ic_round_icon),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else {
                SymbolIcon(
                    name = TasksIcons.WORKSPACE_PREMIUM,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(SettingsContentPadding))

            val isLoading = (state is ProCardState.TasksOrgAccount && state.isLoading)
                    || (state is ProCardState.Subscribed && state.formattedPrice == null)

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
                if (!summary.isNullOrBlank() || isLoading) {
                    Text(
                        text = summary ?: " ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (summary != null)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            Color.Transparent,
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    strokeWidth = 2.dp,
                )
            } else if (showError) {
                SymbolIcon(
                    name = TasksIcons.ERROR_OUTLINE,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = errorColor
                )
            } else if (showChevron) {
                SymbolIcon(
                    name = TasksIcons.KEYBOARD_ARROW_RIGHT,
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

@Composable
fun ManageSubscriptionSheetContent(
    onUpgrade: () -> Unit,
    onModify: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    showUpgrade: Boolean = true,
    showModify: Boolean = true,
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
                iconOverride = TasksIcons.ROCKET_LAUNCH,
                showChevron = false,
            )
        }
        if (showModify) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(Res.string.manage_subscription),
                    icon = TasksIcons.EDIT,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onModify,
                )
            }
        }
        DangerCard(
            icon = TasksIcons.REMOVE_CIRCLE_OUTLINE,
            title = stringResource(Res.string.button_unsubscribe),
            tint = MaterialTheme.colorScheme.error,
            trailingIcon = TasksIcons.OPEN_IN_NEW,
            onClick = onCancel,
        )
    }
}
