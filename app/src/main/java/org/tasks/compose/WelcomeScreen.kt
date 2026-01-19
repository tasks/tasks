package org.tasks.compose

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.themes.TasksTheme

@Composable
fun WelcomeScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onContinueWithoutSync: () -> Unit,
    onImportBackup: () -> Unit,
    openLegalUrl: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val isCompact = maxHeight < 500.dp
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
                            painter = painterResource(id = R.drawable.ic_round_icon),
                            contentDescription = stringResource(R.string.tasks_org),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    WelcomeContent(
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
                        painter = painterResource(id = R.drawable.ic_round_icon),
                        contentDescription = stringResource(R.string.tasks_org),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(152.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    WelcomeContent(
                        onSignIn = onSignIn,
                        onContinueWithoutSync = onContinueWithoutSync,
                        onImportBackup = onImportBackup,
                        openLegalUrl = openLegalUrl,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
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
        LegalDisclosure(
            prefixRes = R.string.legal_disclosure_prefix_continuing,
            openLegalUrl = openLegalUrl,
            modifier = Modifier.widthIn(max = 400.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSignIn,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(R.string.add_account),
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
                text = stringResource(R.string.continue_without_sync),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onImportBackup,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(R.string.returning_user_import_backup),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun LegalDisclosure(
    @StringRes prefixRes: Int,
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
    val prefixText = stringResource(prefixRes)
    val tosText = stringResource(R.string.terms_of_service_proper)
    val privacyText = stringResource(R.string.privacy_policy_proper)
    val licenseText = stringResource(R.string.gplv3_license)
    val template = stringResource(R.string.legal_disclosure)
    val formatted = String.format(template, prefixText, tosText, privacyText, licenseText)

    val tosStart = formatted.indexOf(tosText)
    val tosEnd = tosStart + tosText.length
    val privacyStart = formatted.indexOf(privacyText)
    val privacyEnd = privacyStart + privacyText.length
    val licenseStart = formatted.indexOf(licenseText)
    val licenseEnd = licenseStart + licenseText.length

    val tosUrl = stringResource(R.string.url_tos)
    val privacyUrl = stringResource(R.string.url_privacy_policy)
    val licenseUrl = stringResource(R.string.url_license)

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

@PreviewLightDark
@PreviewScreenSizes
@PreviewFontScale
@Composable
fun WelcomeScreenPreview() {
    TasksTheme {
        WelcomeScreen(
            onBack = {},
            onSignIn = {},
            onContinueWithoutSync = {},
            onImportBackup = {},
            openLegalUrl = {},
        )
    }
}
