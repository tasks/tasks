package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.kmp.JvmBuildConfig
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.contact_developer
import tasks.kmp.generated.resources.documentation
import tasks.kmp.generated.resources.gplv3_license
import tasks.kmp.generated.resources.issue_tracker
import tasks.kmp.generated.resources.legal
import tasks.kmp.generated.resources.privacy_policy
import tasks.kmp.generated.resources.source_code
import tasks.kmp.generated.resources.support
import tasks.kmp.generated.resources.version_string
import tasks.kmp.generated.resources.whats_new

@Composable
fun HelpAndFeedbackScreen(
    versionName: String = JvmBuildConfig.VERSION_NAME,
    onDocumentation: () -> Unit = {},
    onIssueTracker: () -> Unit = {},
    onContactDeveloper: () -> Unit = {},
    onSourceCode: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Version info
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsItemCard(position = CardPosition.Only) {
                PreferenceRow(
                    title = stringResource(Res.string.whats_new),
                    icon = Icons.Outlined.NewReleases,
                    summary = stringResource(Res.string.version_string, versionName),
                    onClick = onDocumentation,
                )
            }
        }

        // Support section
        SectionHeader(
            stringResource(Res.string.support),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(Res.string.documentation),
                    icon = Icons.Outlined.HelpOutline,
                    onClick = onDocumentation,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(Res.string.issue_tracker),
                    icon = Icons.Outlined.BugReport,
                    onClick = onIssueTracker,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(Res.string.contact_developer),
                    icon = Icons.Outlined.Email,
                    onClick = onContactDeveloper,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.source_code),
                    icon = Icons.Outlined.NewReleases,
                    summary = stringResource(Res.string.gplv3_license),
                    onClick = onSourceCode,
                )
            }
        }

        // Legal section
        SectionHeader(
            stringResource(Res.string.legal),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsItemCard(position = CardPosition.Only) {
                PreferenceRow(
                    title = stringResource(Res.string.privacy_policy),
                    icon = Icons.Outlined.PermIdentity,
                    onClick = onPrivacyPolicy,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
