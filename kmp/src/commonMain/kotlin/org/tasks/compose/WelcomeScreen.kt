package org.tasks.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.continue_without_sync
import tasks.kmp.generated.resources.gplv3_license
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.legal_disclosure
import tasks.kmp.generated.resources.legal_disclosure_prefix_continuing
import tasks.kmp.generated.resources.privacy_policy_proper
import tasks.kmp.generated.resources.returning_user_import_backup
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.terms_of_service_proper
import tasks.kmp.generated.resources.url_license
import tasks.kmp.generated.resources.url_privacy_policy
import org.tasks.auth.TasksServerEnvironment
import tasks.kmp.generated.resources.url_tos

@Composable
fun WelcomeScreenLayout(
    showLegalDisclosure: Boolean,
    showImportBackup: Boolean = true,
    onSignIn: () -> Unit,
    onContinueWithoutSync: () -> Unit,
    onImportBackup: () -> Unit = {},
    openLegalUrl: (String) -> Unit,
    environments: List<TasksServerEnvironment.Environment> = emptyList(),
    currentEnvironment: String = TasksServerEnvironment.ENV_PRODUCTION,
    onSelectEnvironment: (String) -> Unit = {},
) {
    var showNetButton by remember { mutableStateOf(currentEnvironment != TasksServerEnvironment.ENV_PRODUCTION) }
    var tapCount by remember { mutableIntStateOf(0) }
    var showEnvironmentSelector by remember { mutableStateOf(false) }

    if (showEnvironmentSelector) {
        EnvironmentSelectorDialog(
            environments = environments,
            currentEnvironment = currentEnvironment,
            onSelect = { env ->
                onSelectEnvironment(env)
                showEnvironmentSelector = false
            },
            onDismiss = { showEnvironmentSelector = false },
        )
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxHeight < 500.dp
                val iconModifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        tapCount++
                        if (tapCount >= 7) {
                            tapCount = 0
                            showNetButton = true
                        }
                    }
                if (isCompact) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_round_icon),
                                contentDescription = stringResource(Res.string.tasks_org),
                                tint = Color.Unspecified,
                                modifier = iconModifier.size(120.dp)
                            )
                        }
                        WelcomeContent(
                            showLegalDisclosure = showLegalDisclosure,
                            showImportBackup = showImportBackup,
                            onSignIn = onSignIn,
                            onContinueWithoutSync = onContinueWithoutSync,
                            onImportBackup = onImportBackup,
                            openLegalUrl = openLegalUrl,
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(Res.drawable.ic_round_icon),
                            contentDescription = stringResource(Res.string.tasks_org),
                            tint = Color.Unspecified,
                            modifier = iconModifier.size(152.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        WelcomeContent(
                            showLegalDisclosure = showLegalDisclosure,
                            showImportBackup = showImportBackup,
                            onSignIn = onSignIn,
                            onContinueWithoutSync = onContinueWithoutSync,
                            onImportBackup = onImportBackup,
                            openLegalUrl = openLegalUrl,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (showNetButton) {
                Text(
                    text = "\u03C0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clickable { showEnvironmentSelector = true },
                )
            }
        }
    }
}

@Composable
private fun EnvironmentSelectorDialog(
    environments: List<TasksServerEnvironment.Environment>,
    currentEnvironment: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = dialogColor,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                environments.forEach { env ->
                    val selected = env.key == currentEnvironment
                    OutlinedCard(
                        onClick = { onSelect(env.key) },
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = dialogColor,
                        ),
                    ) {
                        Text(
                            text = env.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    showLegalDisclosure: Boolean,
    showImportBackup: Boolean,
    onSignIn: () -> Unit,
    onContinueWithoutSync: () -> Unit,
    onImportBackup: () -> Unit,
    openLegalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = Modifier
        .widthIn(max = 400.dp)
        .fillMaxWidth()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showLegalDisclosure) {
            LegalDisclosure(
                prefixText = stringResource(Res.string.legal_disclosure_prefix_continuing),
                openLegalUrl = openLegalUrl,
                modifier = Modifier.widthIn(max = 400.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onSignIn,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(Res.string.add_account),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onContinueWithoutSync,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(Res.string.continue_without_sync),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        if (showImportBackup) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onImportBackup,
                modifier = buttonModifier
            ) {
                Text(
                    text = stringResource(Res.string.returning_user_import_backup),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun LegalDisclosure(
    prefixText: String,
    openLegalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val bodyStyle = MaterialTheme.typography.bodySmall.copy(
        textAlign = textAlign,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )
    val tosText = stringResource(Res.string.terms_of_service_proper)
    val privacyText = stringResource(Res.string.privacy_policy_proper)
    val licenseText = stringResource(Res.string.gplv3_license)
    val template = stringResource(Res.string.legal_disclosure)
    val formatted = String.format(template, prefixText, tosText, privacyText, licenseText)

    val tosStart = formatted.indexOf(tosText)
    val tosEnd = tosStart + tosText.length
    val privacyStart = formatted.indexOf(privacyText)
    val privacyEnd = privacyStart + privacyText.length
    val licenseStart = formatted.indexOf(licenseText)
    val licenseEnd = licenseStart + licenseText.length

    val tosUrl = stringResource(Res.string.url_tos)
    val privacyUrl = stringResource(Res.string.url_privacy_policy)
    val licenseUrl = stringResource(Res.string.url_license)

    val annotatedText = buildAnnotatedString {
        append(formatted)
        addStyle(linkStyle, tosStart, tosEnd)
        addStringAnnotation(tag = "url", annotation = tosUrl, start = tosStart, end = tosEnd)
        addStyle(linkStyle, privacyStart, privacyEnd)
        addStringAnnotation(tag = "url", annotation = privacyUrl, start = privacyStart, end = privacyEnd)
        addStyle(linkStyle, licenseStart, licenseEnd)
        addStringAnnotation(tag = "url", annotation = licenseUrl, start = licenseStart, end = licenseEnd)
    }

    ClickableText(
        text = annotatedText,
        style = bodyStyle,
        modifier = modifier,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "url", start = offset, end = offset)
                .firstOrNull()
                ?.let { openLegalUrl(it.item) }
        }
    )
}
