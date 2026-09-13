package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.analytics.AnalyticsEvents.SettingsClick
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.restart_later
import tasks.kmp.generated.resources.restart_now
import tasks.kmp.generated.resources.restart_required

@Composable
fun HelpAndFeedbackContent(
    viewModel: HelpAndFeedbackViewModel,
    openUri: (String) -> Unit,
    onRestartApplication: () -> Unit,
    onRateTasks: (() -> Unit)? = null,
    onContactDeveloper: (() -> Unit)? = null,
    onSendLogs: (() -> Unit)? = null,
    onThirdPartyLicenses: (() -> Unit)? = null,
    onCollectStatisticsChanged: ((Boolean) -> Unit)? = null,
    bottomInsets: @Composable () -> Unit = {},
) {
    HelpAndFeedbackScreen(
        versionName = viewModel.versionName,
        isGooglePlay = viewModel.isGooglePlay,
        showTermsOfService = viewModel.showTermsOfService,
        showSendLogs = viewModel.showSendLogs,
        showCollectStatistics = viewModel.showCollectStatistics,
        collectStatistics = viewModel.collectStatistics,
        onWhatsNew = {
            viewModel.logEvent(SettingsClick.WHATS_NEW)
            openUri(TasksUrls.CHANGELOG)
        },
        onRateTasks = {
            viewModel.logEvent(SettingsClick.RATE_TASKS)
            onRateTasks?.invoke()
        },
        onDocumentation = {
            viewModel.logEvent(SettingsClick.DOCUMENTATION)
            openUri(TasksUrls.DOCUMENTATION)
        },
        onIssueTracker = {
            viewModel.logEvent(SettingsClick.ISSUE_TRACKER)
            openUri(TasksUrls.ISSUE_TRACKER)
        },
        onContactDeveloper = {
            viewModel.logEvent(SettingsClick.CONTACT_DEVELOPER)
            if (onContactDeveloper != null) {
                onContactDeveloper()
            } else {
                openUri("mailto:${TasksUrls.SUPPORT_EMAIL}?subject=Tasks%20Feedback")
            }
        },
        onSendLogs = {
            viewModel.logEvent(SettingsClick.SEND_LOGS)
            onSendLogs?.invoke()
        },
        onReddit = {
            viewModel.logEvent(SettingsClick.REDDIT)
            openUri(TasksUrls.REDDIT)
        },
        onTwitter = {
            viewModel.logEvent(SettingsClick.TWITTER)
            openUri(TasksUrls.TWITTER)
        },
        onSourceCode = {
            viewModel.logEvent(SettingsClick.SOURCE_CODE)
            openUri(TasksUrls.SOURCE_CODE)
        },
        onThirdPartyLicenses = {
            viewModel.logEvent(SettingsClick.THIRD_PARTY_LICENSES)
            onThirdPartyLicenses?.invoke()
        },
        onTermsOfService = {
            viewModel.logEvent(SettingsClick.TOS)
            openUri(TasksUrls.TOS)
        },
        onPrivacyPolicy = {
            viewModel.logEvent(SettingsClick.PRIVACY_POLICY)
            openUri(TasksUrls.PRIVACY_POLICY)
        },
        blogFeedMode = viewModel.blogFeedMode,
        onBlogFeedModeClick = { viewModel.showBlogFeedModeDialog() },
        onCollectStatisticsChanged = { enabled ->
            viewModel.updateCollectStatistics(enabled)
            onCollectStatisticsChanged?.invoke(enabled)
        },
        bottomInsets = bottomInsets,
    )

    if (viewModel.showRestartDialog) {
        ConfirmDialog(
            text = stringResource(Res.string.restart_required),
            confirmText = stringResource(Res.string.restart_now),
            dismissText = stringResource(Res.string.restart_later),
            onConfirm = onRestartApplication,
            onDismiss = { viewModel.dismissRestartDialog() },
        )
    }

    if (viewModel.showBlogFeedModeDialog) {
        BlogFeedModeDialog(
            selected = viewModel.blogFeedMode,
            onSelect = { viewModel.updateBlogFeedMode(it) },
            onDismiss = { viewModel.dismissBlogFeedModeDialog() },
        )
    }
}
