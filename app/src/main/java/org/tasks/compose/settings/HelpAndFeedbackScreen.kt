package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.feed.BlogFeedMode
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun HelpAndFeedbackScreen(
    versionName: String,
    isGooglePlay: Boolean,
    showTermsOfService: Boolean,
    collectStatistics: Boolean,
    onWhatsNew: () -> Unit,
    onRateTasks: () -> Unit,
    onDocumentation: () -> Unit,
    onIssueTracker: () -> Unit,
    onContactDeveloper: () -> Unit,
    onSendLogs: () -> Unit,
    onReddit: () -> Unit,
    onTwitter: () -> Unit,
    onSourceCode: () -> Unit,
    onThirdPartyLicenses: () -> Unit,
    onTermsOfService: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    blogFeedMode: BlogFeedMode,
    onBlogFeedModeClick: () -> Unit,
    onCollectStatisticsChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Top items
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.whats_new),
                    icon = Icons.Outlined.NewReleases,
                    summary = stringResource(R.string.version_string, versionName),
                    onClick = onWhatsNew,
                )
            }
            SettingsItemCard(
                position = if (isGooglePlay) CardPosition.Middle else CardPosition.Last,
            ) {
                PreferenceRow(
                    title = stringResource(R.string.blog_notifications),
                    icon = Icons.Outlined.RssFeed,
                    summary = stringResource(
                        when (blogFeedMode) {
                            BlogFeedMode.NONE -> R.string.blog_feed_none
                            BlogFeedMode.ANNOUNCEMENTS -> R.string.blog_feed_announcements
                            BlogFeedMode.ALL -> R.string.blog_feed_all
                        }
                    ),
                    onClick = onBlogFeedModeClick,
                )
            }
            if (isGooglePlay) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.rate_tasks),
                        icon = Icons.Outlined.StarBorder,
                        onClick = onRateTasks,
                    )
                }
            }
        }

        // Support section
        SectionHeader(
            R.string.support,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.documentation),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = onDocumentation,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.issue_tracker),
                    icon = Icons.Outlined.BugReport,
                    onClick = onIssueTracker,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.contact_developer),
                    icon = Icons.Outlined.Email,
                    onClick = onContactDeveloper,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.send_application_logs),
                    icon = Icons.Outlined.Attachment,
                    onClick = onSendLogs,
                )
            }
        }

        // Social section
        SectionHeader(
            R.string.social,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.follow_reddit),
                    iconRes = R.drawable.ic_reddit_share_silhouette,
                    onClick = onReddit,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.follow_twitter),
                    iconRes = R.drawable.ic_twitter_logo_black,
                    onClick = onTwitter,
                )
            }
        }

        // Open Source section
        SectionHeader(
            R.string.open_source,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(
                position = if (isGooglePlay) CardPosition.First else CardPosition.Only,
            ) {
                PreferenceRow(
                    title = stringResource(R.string.source_code),
                    iconRes = R.drawable.ic_octocat,
                    summary = stringResource(R.string.license_summary),
                    onClick = onSourceCode,
                )
            }
            if (isGooglePlay) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.third_party_licenses),
                        icon = Icons.Outlined.Gavel,
                        onClick = onThirdPartyLicenses,
                    )
                }
            }
        }

        // Legal section
        SectionHeader(
            R.string.legal,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            if (showTermsOfService) {
                SettingsItemCard(position = CardPosition.First) {
                    PreferenceRow(
                        title = stringResource(R.string.terms_of_service),
                        icon = Icons.Outlined.Gavel,
                        onClick = onTermsOfService,
                    )
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.privacy_policy),
                        icon = Icons.Outlined.PermIdentity,
                        onClick = onPrivacyPolicy,
                    )
                }
            } else {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(R.string.privacy_policy),
                        icon = Icons.Outlined.PermIdentity,
                        onClick = onPrivacyPolicy,
                    )
                }
            }
        }

        // Statistics toggle
        if (isGooglePlay) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.send_anonymous_statistics),
                    icon = Icons.Outlined.BugReport,
                    summary = stringResource(R.string.send_anonymous_statistics_summary),
                    checked = collectStatistics,
                    onCheckedChange = onCollectStatisticsChanged,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
fun BlogFeedModeDialog(
    selected: BlogFeedMode,
    onSelect: (BlogFeedMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        BlogFeedMode.NONE to stringResource(R.string.blog_feed_none),
        BlogFeedMode.ANNOUNCEMENTS to stringResource(R.string.blog_feed_announcements),
        BlogFeedMode.ALL to stringResource(R.string.blog_feed_all),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blog_notifications)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
