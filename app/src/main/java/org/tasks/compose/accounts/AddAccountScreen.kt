package org.tasks.compose.accounts

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import org.tasks.compose.LegalDisclosure
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    hasTasksAccount: Boolean,
    hasPro: Boolean,
    needsConsent: Boolean,
    onBack: () -> Unit,
    signIn: (Platform) -> Unit,
    openUrl: (Platform) -> Unit,
    openLegalUrl: (String) -> Unit,
    onConsent: suspend () -> Unit = {},
    onNameYourPriceInfo: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showNameYourPriceInfo by remember { mutableStateOf(false) }
    var showTasksOrgConsent by remember { mutableStateOf(false) }

    if (showNameYourPriceInfo) {
        AlertDialog(
            onDismissRequest = { showNameYourPriceInfo = false },
            title = { Text(stringResource(R.string.name_your_price)) },
            text = { Text(stringResource(R.string.name_your_price_blurb)) },
            confirmButton = {
                TextButton(onClick = { showNameYourPriceInfo = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    if (showTasksOrgConsent) {
        AlertDialog(
            onDismissRequest = { showTasksOrgConsent = false },
            title = { Text(stringResource(R.string.terms_of_service_proper)) },
            text = {
                LegalDisclosure(
                    prefixRes = R.string.legal_disclosure_prefix_using,
                    openLegalUrl = openLegalUrl,
                    textAlign = TextAlign.Start,
                )
            },
            dismissButton = {
                TextButton(onClick = { showTasksOrgConsent = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showTasksOrgConsent = false
                    scope.launch {
                        onConsent()
                        signIn(Platform.TASKS_ORG)
                    }
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
        )
    }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
            ) {
                if (!hasTasksAccount) {
                    SectionHeader(R.string.upgrade_to_pro)
                    AccountTypeCard(
                        title = R.string.tasks_org,
                        icon = R.drawable.ic_round_icon,
                        description = R.string.tasks_org_description,
                        onClick = {
                            if (needsConsent) {
                                showTasksOrgConsent = true
                            } else {
                                signIn(Platform.TASKS_ORG)
                            }
                        }
                    )
                    if (hasPro) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                if (!hasPro) {
                    SectionHeader(R.string.cost_free)
                }
                CardGroup {
                    AccountTypeRow(
                        title = R.string.microsoft,
                        icon = R.drawable.ic_microsoft_tasks,
                        description = if (IS_GOOGLE_PLAY)
                            R.string.microsoft_selection_description_googleplay
                        else
                            R.string.microsoft_selection_description,
                        onClick = { signIn(Platform.MICROSOFT) }
                    )
                    HorizontalDivider()
                    AccountTypeRow(
                        title = R.string.gtasks_GPr_header,
                        icon = R.drawable.ic_google,
                        description = R.string.google_tasks_selection_description,
                        onClick = { signIn(Platform.GOOGLE_TASKS) }
                    )
                }

                if (!hasPro) {
                    SectionHeader(
                        title = R.string.name_your_price,
                        onInfoClick = {
                            onNameYourPriceInfo()
                            showNameYourPriceInfo = true
                        },
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                CardGroup {
                    AccountTypeRow(
                        title = R.string.davx5,
                        icon = R.drawable.ic_davx5_icon_green_bg,
                        description = R.string.davx5_selection_description,
                        onClick = { openUrl(Platform.DAVX5) }
                    )
                    HorizontalDivider()
                    AccountTypeRow(
                        title = R.string.caldav,
                        icon = R.drawable.ic_webdav_logo,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                        description = R.string.caldav_selection_description,
                        onClick = { signIn(Platform.CALDAV) }
                    )
                    HorizontalDivider()
                    AccountTypeRow(
                        title = R.string.etesync,
                        icon = R.drawable.ic_etesync,
                        description = R.string.etesync_selection_description,
                        onClick = { signIn(Platform.ETEBASE) }
                    )
                    HorizontalDivider()
                    AccountTypeRow(
                        title = R.string.decsync,
                        icon = R.drawable.ic_decsync,
                        description = R.string.decsync_selection_description,
                        onClick = { openUrl(Platform.DECSYNC_CC) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    @StringRes title: Int,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .then(
                if (onInfoClick != null)
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onInfoClick,
                    )
                else
                    Modifier
            ),
    ) {
        Text(
            text = stringResource(id = title).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onInfoClick != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.help),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp),
            )
        }
    }
}

@Composable
private fun CardGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(content = content)
    }
}

@Composable
private fun AccountTypeCard(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        AccountTypeRowContent(
            title = title,
            icon = icon,
            description = description,
            tint = tint,
        )
    }
}

@Composable
private fun AccountTypeRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AccountTypeRowContent(
            title = title,
            icon = icon,
            description = description,
            tint = tint,
        )
    }
}

@Composable
private fun AccountTypeRowContent(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    tint: Color? = null,
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
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(id = description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
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
            needsConsent = false,
            onBack = {},
            signIn = {},
            openUrl = {},
            openLegalUrl = {},
        )
    }
}

@PreviewLightDark
@Composable
fun NameYourPriceDialogPreview() {
    TasksTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.name_your_price)) },
            text = { Text(stringResource(R.string.name_your_price_blurb)) },
            confirmButton = {
                TextButton(onClick = {}) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
