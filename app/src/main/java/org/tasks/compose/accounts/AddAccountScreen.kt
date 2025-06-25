package org.tasks.compose.accounts

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.sync.AddAccountDialog.Platform
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountScreen(
    gettingStarted: Boolean,
    hasTasksAccount: Boolean,
    hasPro: Boolean,
    onBack: () -> Unit,
    signIn: (Platform) -> Unit,
    openUrl: (Platform) -> Unit,
    onImportBackup: () -> Unit,
) {
    BackHandler {
        if (!gettingStarted) {
            onBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(),
                navigationIcon = {
                    if (!gettingStarted) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = if (gettingStarted) {
                            stringResource(R.string.sign_in)
                        } else {
                            stringResource(R.string.add_account)
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 5
            ) {
                if (gettingStarted) {
                    ActionCard(
                        title = R.string.backup_BAc_import,
                        icon = Icons.Outlined.Backup,
                        onClick = onImportBackup,
                        isOutlined = true
                    )

                    ActionCard(
                        title = R.string.continue_without_sync,
                        icon = Icons.Outlined.CloudOff,
                        onClick = { signIn(Platform.LOCAL) },
                        isOutlined = true
                    )
                }
                if (!hasTasksAccount) {
                    AccountTypeCard(
                        title = R.string.tasks_org,
                        cost = R.string.cost_more_money,
                        icon = R.drawable.ic_round_icon,
                        onClick = { signIn(Platform.TASKS_ORG) }
                    )
                }
                
                AccountTypeCard(
                    title = R.string.microsoft,
                    cost = if (hasPro) null else R.string.cost_free,
                    icon = R.drawable.ic_microsoft_tasks,
                    onClick = { signIn(Platform.MICROSOFT) }
                )

                AccountTypeCard(
                    title = R.string.todoist,
                    cost = if (hasPro) null else R.string.cost_free,
                    icon = R.drawable.ic_todoist,
                    onClick = { signIn(Platform.TODOIST) }
                )

                AccountTypeCard(
                    title = R.string.gtasks_GPr_header,
                    cost = if (hasPro) null else R.string.cost_free,
                    icon = R.drawable.ic_google,
                    onClick = { signIn(Platform.GOOGLE_TASKS) }
                )
                
                AccountTypeCard(
                    title = R.string.davx5,
                    cost = if (hasPro) null else R.string.cost_money,
                    icon = R.drawable.ic_davx5_icon_green_bg,
                    onClick = { openUrl(Platform.DAVX5) }
                )
                
                AccountTypeCard(
                    title = R.string.caldav,
                    cost = if (hasPro) null else R.string.cost_money,
                    icon = R.drawable.ic_webdav_logo,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                    onClick = { signIn(Platform.CALDAV) }
                )
                
                AccountTypeCard(
                    title = R.string.etesync,
                    cost = if (hasPro) null else R.string.cost_money,
                    icon = R.drawable.ic_etesync,
                    onClick = { signIn(Platform.ETESYNC) }
                )
                
                AccountTypeCard(
                    title = R.string.decsync,
                    cost = if (hasPro) null else R.string.cost_money,
                    icon = R.drawable.ic_decsync,
                    onClick = { openUrl(Platform.DECSYNC_CC) }
                )

                if (gettingStarted) {
                    ActionCard(
                        title = R.string.help_me_choose,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = { openUrl(Platform.LOCAL) },
                        isOutlined = true
                    )
                }
            }
        }
    }
}

@Composable
fun AccountTypeCard(
    @StringRes title: Int,
    @StringRes cost: Int? = null,
    @DrawableRes icon: Int,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(108.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = stringResource(id = title),
                tint = tint ?: Color.Unspecified,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = buildAnnotatedString {
                    append(stringResource(id = title))
                    cost?.let {
                        append("\n")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                        ) {
                            append(stringResource(id = it))
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ActionCard(
    @StringRes title: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    isOutlined: Boolean = false
) {
    if (isOutlined) {
        OutlinedCard(
            modifier = Modifier
                .width(108.dp),
            shape = MaterialTheme.shapes.medium,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    minLines = 3,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .width(150.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@PreviewFontScale
@Composable
fun GettingStartedPreview() {
    TasksTheme {
        AddAccountScreen(
            gettingStarted = true,
            hasTasksAccount = false,
            hasPro = false,
            onBack = {},
            signIn = {},
            openUrl = {},
            onImportBackup = {},
        )
    }
}

@PreviewLightDark
@Composable
fun AddAccountPreview() {
    TasksTheme {
        AddAccountScreen(
            gettingStarted = false,
            hasTasksAccount = false,
            hasPro = false,
            onBack = {},
            signIn = {},
            openUrl = {},
            onImportBackup = {},
        )
    }
}
