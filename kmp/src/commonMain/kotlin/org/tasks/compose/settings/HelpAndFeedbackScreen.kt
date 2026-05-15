package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.feed.BlogFeedMode
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.blog_feed_all
import tasks.kmp.generated.resources.blog_feed_announcements
import tasks.kmp.generated.resources.blog_feed_none
import tasks.kmp.generated.resources.blog_notifications
import tasks.kmp.generated.resources.contact_developer
import tasks.kmp.generated.resources.documentation
import tasks.kmp.generated.resources.follow_reddit
import tasks.kmp.generated.resources.follow_twitter
import tasks.kmp.generated.resources.ic_octocat
import tasks.kmp.generated.resources.ic_reddit_share_silhouette
import tasks.kmp.generated.resources.ic_x_logo_black
import tasks.kmp.generated.resources.issue_tracker
import tasks.kmp.generated.resources.legal
import tasks.kmp.generated.resources.license_summary
import tasks.kmp.generated.resources.open_source
import tasks.kmp.generated.resources.privacy_policy
import tasks.kmp.generated.resources.rate_tasks
import tasks.kmp.generated.resources.send_anonymous_statistics
import tasks.kmp.generated.resources.send_anonymous_statistics_summary
import tasks.kmp.generated.resources.send_application_logs
import tasks.kmp.generated.resources.social
import tasks.kmp.generated.resources.source_code
import tasks.kmp.generated.resources.support
import tasks.kmp.generated.resources.terms_of_service
import tasks.kmp.generated.resources.third_party_licenses
import tasks.kmp.generated.resources.version_string
import tasks.kmp.generated.resources.whats_new

@Composable
fun HelpAndFeedbackScreen(
    versionName: String,
    isGooglePlay: Boolean,
    showTermsOfService: Boolean,
    showSendLogs: Boolean,
    showCollectStatistics: Boolean,
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
    bottomInsets: @Composable () -> Unit = {},
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
                    title = stringResource(Res.string.whats_new),
                    icon = Icons.Outlined.NewReleases,
                    summary = stringResource(Res.string.version_string, versionName),
                    onClick = onWhatsNew,
                )
            }
            SettingsItemCard(
                position = if (isGooglePlay) CardPosition.Middle else CardPosition.Last,
            ) {
                PreferenceRow(
                    title = stringResource(Res.string.blog_notifications),
                    icon = Icons.Outlined.RssFeed,
                    summary = stringResource(
                        when (blogFeedMode) {
                            BlogFeedMode.NONE -> Res.string.blog_feed_none
                            BlogFeedMode.ANNOUNCEMENTS -> Res.string.blog_feed_announcements
                            BlogFeedMode.ALL -> Res.string.blog_feed_all
                        }
                    ),
                    onClick = onBlogFeedModeClick,
                )
            }
            if (isGooglePlay) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.rate_tasks),
                        icon = Icons.Outlined.StarBorder,
                        onClick = onRateTasks,
                    )
                }
            }
        }

        // Support section
        SectionHeader(
            stringResource(Res.string.support),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(Res.string.documentation),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
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
            SettingsItemCard(
                position = if (showSendLogs) CardPosition.Middle else CardPosition.Last,
            ) {
                PreferenceRow(
                    title = stringResource(Res.string.contact_developer),
                    icon = Icons.Outlined.Email,
                    onClick = onContactDeveloper,
                )
            }
            if (showSendLogs) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.send_application_logs),
                        icon = Icons.Outlined.Attachment,
                        onClick = onSendLogs,
                    )
                }
            }
        }

        // Social section
        SectionHeader(
            stringResource(Res.string.social),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(Res.string.follow_reddit),
                    iconDrawable = Res.drawable.ic_reddit_share_silhouette,
                    onClick = onReddit,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.follow_twitter),
                    iconDrawable = Res.drawable.ic_x_logo_black,
                    onClick = onTwitter,
                )
            }
        }

        // Open Source section
        SectionHeader(
            stringResource(Res.string.open_source),
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
                    title = stringResource(Res.string.source_code),
                    iconDrawable = Res.drawable.ic_octocat,
                    summary = stringResource(Res.string.license_summary),
                    onClick = onSourceCode,
                )
            }
            if (isGooglePlay) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.third_party_licenses),
                        icon = Icons.Outlined.Gavel,
                        onClick = onThirdPartyLicenses,
                    )
                }
            }
        }

        // Legal section
        SectionHeader(
            stringResource(Res.string.legal),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            if (showTermsOfService) {
                SettingsItemCard(position = CardPosition.First) {
                    PreferenceRow(
                        title = stringResource(Res.string.terms_of_service),
                        icon = Icons.Outlined.Gavel,
                        onClick = onTermsOfService,
                    )
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.privacy_policy),
                        icon = Icons.Outlined.PermIdentity,
                        onClick = onPrivacyPolicy,
                    )
                }
            } else {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(Res.string.privacy_policy),
                        icon = Icons.Outlined.PermIdentity,
                        onClick = onPrivacyPolicy,
                    )
                }
            }
        }

        // Statistics toggle
        if (showCollectStatistics) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.send_anonymous_statistics),
                    icon = Icons.Outlined.BugReport,
                    summary = stringResource(Res.string.send_anonymous_statistics_summary),
                    checked = collectStatistics,
                    onCheckedChange = onCollectStatisticsChanged,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        bottomInsets()
    }
}

@Composable
fun BlogFeedModeDialog(
    selected: BlogFeedMode,
    onSelect: (BlogFeedMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        BlogFeedMode.NONE to stringResource(Res.string.blog_feed_none),
        BlogFeedMode.ANNOUNCEMENTS to stringResource(Res.string.blog_feed_announcements),
        BlogFeedMode.ALL to stringResource(Res.string.blog_feed_all),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.blog_notifications)) },
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

