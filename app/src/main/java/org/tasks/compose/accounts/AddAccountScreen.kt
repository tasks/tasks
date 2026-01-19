package org.tasks.compose.accounts

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    hasTasksAccount: Boolean,
    hasPro: Boolean,
    onBack: () -> Unit,
    signIn: (Platform) -> Unit,
    openUrl: (Platform) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.add_account))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!hasTasksAccount) {
                    SectionHeader(R.string.upgrade_to_pro)
                    AccountTypeCard(
                        title = R.string.tasks_org,
                        icon = R.drawable.ic_round_icon,
                        description = R.string.tasks_org_description,
                        onClick = { signIn(Platform.TASKS_ORG) }
                    )
                    if (hasPro) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                if (!hasPro) {
                    SectionHeader(R.string.cost_free)
                }
                AccountTypeCard(
                    title = R.string.microsoft,
                    icon = R.drawable.ic_microsoft_tasks,
                    description = if (IS_GOOGLE_PLAY)
                        R.string.microsoft_selection_description_googleplay
                    else
                        R.string.microsoft_selection_description,
                    onClick = { signIn(Platform.MICROSOFT) }
                )
                AccountTypeCard(
                    title = R.string.gtasks_GPr_header,
                    icon = R.drawable.ic_google,
                    description = R.string.google_tasks_selection_description,
                    onClick = { signIn(Platform.GOOGLE_TASKS) }
                )

                if (!hasPro) {
                    SectionHeader(R.string.name_your_price)
                }
                AccountTypeCard(
                    title = R.string.davx5,
                    icon = R.drawable.ic_davx5_icon_green_bg,
                    description = R.string.davx5_selection_description,
                    onClick = { openUrl(Platform.DAVX5) }
                )
                AccountTypeCard(
                    title = R.string.caldav,
                    icon = R.drawable.ic_webdav_logo,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                    description = R.string.caldav_selection_description,
                    onClick = { signIn(Platform.CALDAV) }
                )
                AccountTypeCard(
                    title = R.string.etesync,
                    icon = R.drawable.ic_etesync,
                    description = R.string.etesync_selection_description,
                    onClick = { signIn(Platform.ETESYNC) }
                )
                AccountTypeCard(
                    title = R.string.decsync,
                    icon = R.drawable.ic_decsync,
                    description = R.string.decsync_selection_description,
                    onClick = { openUrl(Platform.DECSYNC_CC) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(@StringRes title: Int) {
    Text(
        text = stringResource(id = title).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AccountTypeCard(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    @StringRes price: Int? = null,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint ?: Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    price?.let {
                        Text(
                            text = stringResource(id = it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = stringResource(id = description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@PreviewFontScale
@Composable
fun AddAccountPreview() {
    TasksTheme {
        AddAccountScreen(
            hasTasksAccount = false,
            hasPro = false,
            onBack = {},
            signIn = {},
            openUrl = {},
        )
    }
}
