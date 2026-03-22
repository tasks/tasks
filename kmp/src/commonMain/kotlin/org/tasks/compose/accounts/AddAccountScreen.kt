package org.tasks.compose.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.PlatformConfiguration
import org.tasks.compose.LegalDisclosure
import org.tasks.compose.settings.CardPosition
import org.tasks.compose.settings.SectionHeader
import org.tasks.compose.settings.SettingsCardGap
import org.tasks.compose.settings.SettingsContentPadding
import org.tasks.compose.settings.SettingsItemCard
import org.tasks.compose.settings.SettingsRowPadding
import org.tasks.compose.settings.SettingsSectionGap
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.accept
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.caldav
import tasks.kmp.generated.resources.caldav_selection_description
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.cost_free
import tasks.kmp.generated.resources.davx5
import tasks.kmp.generated.resources.davx5_selection_description
import tasks.kmp.generated.resources.decsync
import tasks.kmp.generated.resources.decsync_selection_description
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.etesync_selection_description
import tasks.kmp.generated.resources.google_tasks_selection_description
import tasks.kmp.generated.resources.gtasks_GPr_header
import tasks.kmp.generated.resources.ic_davx5_icon_green_bg
import tasks.kmp.generated.resources.ic_decsync
import tasks.kmp.generated.resources.ic_etesync
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_microsoft_tasks
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.ic_webdav_logo
import tasks.kmp.generated.resources.legal_disclosure_prefix_using
import tasks.kmp.generated.resources.microsoft
import tasks.kmp.generated.resources.microsoft_selection_description
import tasks.kmp.generated.resources.microsoft_selection_description_googleplay
import tasks.kmp.generated.resources.name_your_price
import tasks.kmp.generated.resources.name_your_price_blurb
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.tasks_org_account
import tasks.kmp.generated.resources.tasks_org_description
import tasks.kmp.generated.resources.terms_of_service_proper
import tasks.kmp.generated.resources.upgrade_to_pro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    configuration: PlatformConfiguration,
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
            title = { Text(stringResource(Res.string.name_your_price)) },
            text = { Text(stringResource(Res.string.name_your_price_blurb)) },
            confirmButton = {
                TextButton(onClick = { showNameYourPriceInfo = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }

    if (showTasksOrgConsent) {
        AlertDialog(
            onDismissRequest = { showTasksOrgConsent = false },
            title = { Text(stringResource(Res.string.terms_of_service_proper)) },
            text = {
                LegalDisclosure(
                    prefixText = stringResource(Res.string.legal_disclosure_prefix_using),
                    openLegalUrl = openLegalUrl,
                    textAlign = TextAlign.Start,
                )
            },
            dismissButton = {
                TextButton(onClick = { showTasksOrgConsent = false }) {
                    Text(stringResource(Res.string.cancel))
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
                    Text(stringResource(Res.string.accept))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(Res.string.add_account))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
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
                    .padding(horizontal = SettingsContentPadding),
            ) {
                if (configuration.supportsTasksOrg && !hasTasksAccount) {
                    if (!hasPro) {
                        SectionHeader(stringResource(Res.string.upgrade_to_pro))
                    } else {
                        Spacer(modifier = Modifier.height(SettingsSectionGap))
                    }
                    SettingsItemCard {
                        AccountTypeRow(
                            title = stringResource(Res.string.tasks_org_account),
                            icon = Res.drawable.ic_round_icon,
                            description = stringResource(Res.string.tasks_org_description),
                            onClick = {
                                if (needsConsent) {
                                    showTasksOrgConsent = true
                                } else {
                                    signIn(Platform.TASKS_ORG)
                                }
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(SettingsSectionGap))
                }

                val freeAccounts = buildList {
                    if (configuration.supportsMicrosoft) add(Platform.MICROSOFT)
                    if (configuration.supportsGoogleTasks) add(Platform.GOOGLE_TASKS)
                }
                if (freeAccounts.isNotEmpty()) {
                    if (!hasPro) {
                        SectionHeader(stringResource(Res.string.cost_free))
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
                    ) {
                        freeAccounts.forEachIndexed { index, platform ->
                            SettingsItemCard(
                                position = CardPosition.forIndex(index, freeAccounts.size),
                            ) {
                                when (platform) {
                                    Platform.MICROSOFT -> AccountTypeRow(
                                        title = stringResource(Res.string.microsoft),
                                        icon = Res.drawable.ic_microsoft_tasks,
                                        description = stringResource(
                                            if (!configuration.isLibre)
                                                Res.string.microsoft_selection_description_googleplay
                                            else
                                                Res.string.microsoft_selection_description
                                        ),
                                        onClick = { signIn(Platform.MICROSOFT) },
                                    )
                                    Platform.GOOGLE_TASKS -> AccountTypeRow(
                                        title = stringResource(Res.string.gtasks_GPr_header),
                                        icon = Res.drawable.ic_google,
                                        description = stringResource(Res.string.google_tasks_selection_description),
                                        onClick = { signIn(Platform.GOOGLE_TASKS) },
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                val proAccounts = buildList {
                    if (configuration.supportsOpenTasks) add(Platform.DAVX5)
                    if (configuration.supportsCaldav) add(Platform.CALDAV)
                    if (configuration.supportsEteSync) add(Platform.ETEBASE)
                    if (configuration.supportsOpenTasks) add(Platform.DECSYNC_CC)
                }
                if (proAccounts.isNotEmpty()) {
                    if (!hasPro) {
                        SectionHeader(
                            title = stringResource(Res.string.name_your_price),
                            onClick = {
                                onNameYourPriceInfo()
                                showNameYourPriceInfo = true
                            },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(SettingsContentPadding))
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
                    ) {
                        proAccounts.forEachIndexed { index, platform ->
                            SettingsItemCard(
                                position = CardPosition.forIndex(index, proAccounts.size),
                            ) {
                                when (platform) {
                                    Platform.DAVX5 -> AccountTypeRow(
                                        title = stringResource(Res.string.davx5),
                                        icon = Res.drawable.ic_davx5_icon_green_bg,
                                        description = stringResource(Res.string.davx5_selection_description),
                                        onClick = { openUrl(Platform.DAVX5) },
                                    )
                                    Platform.CALDAV -> AccountTypeRow(
                                        title = stringResource(Res.string.caldav),
                                        icon = Res.drawable.ic_webdav_logo,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                                        description = stringResource(Res.string.caldav_selection_description),
                                        onClick = { signIn(Platform.CALDAV) },
                                    )
                                    Platform.ETEBASE -> AccountTypeRow(
                                        title = stringResource(Res.string.etesync),
                                        icon = Res.drawable.ic_etesync,
                                        description = stringResource(Res.string.etesync_selection_description),
                                        onClick = { signIn(Platform.ETEBASE) },
                                    )
                                    Platform.DECSYNC_CC -> AccountTypeRow(
                                        title = stringResource(Res.string.decsync),
                                        icon = Res.drawable.ic_decsync,
                                        description = stringResource(Res.string.decsync_selection_description),
                                        onClick = { openUrl(Platform.DECSYNC_CC) },
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SettingsContentPadding))
                Spacer(modifier = Modifier.height(48.dp)) // navigation bar clearance
            }
        }
    }
}

@Composable
private fun AccountTypeRow(
    title: String,
    icon: DrawableResource,
    description: String,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SettingsContentPadding, vertical = SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint ?: Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
