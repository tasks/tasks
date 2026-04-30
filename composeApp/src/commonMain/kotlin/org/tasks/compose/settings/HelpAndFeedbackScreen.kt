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
import org.tasks.kmp.JvmBuildConfig

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
                    title = "What's New",
                    icon = Icons.Outlined.NewReleases,
                    summary = "Version $versionName",
                    onClick = onDocumentation,
                )
            }
        }

        // Support section
        SectionHeader(
            "Support",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = "Documentation",
                    icon = Icons.Outlined.HelpOutline,
                    onClick = onDocumentation,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = "Issue Tracker",
                    icon = Icons.Outlined.BugReport,
                    onClick = onIssueTracker,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = "Contact Developer",
                    icon = Icons.Outlined.Email,
                    onClick = onContactDeveloper,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = "Source Code",
                    icon = Icons.Outlined.NewReleases,
                    summary = "GPLv3 License",
                    onClick = onSourceCode,
                )
            }
        }

        // Legal section
        SectionHeader(
            "Legal",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsItemCard(position = CardPosition.Only) {
                PreferenceRow(
                    title = "Privacy Policy",
                    icon = Icons.Outlined.PermIdentity,
                    onClick = onPrivacyPolicy,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
