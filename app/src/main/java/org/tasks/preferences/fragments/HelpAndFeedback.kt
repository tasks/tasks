package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.settings.HelpAndFeedbackScreen
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class HelpAndFeedback : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: HelpAndFeedbackViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            HelpAndFeedbackScreen(
                versionName = viewModel.versionName,
                isGooglePlay = viewModel.isGooglePlay,
                showTermsOfService = viewModel.showTermsOfService,
                collectStatistics = viewModel.collectStatistics,
                onWhatsNew = {
                    viewModel.logEvent("whats_new")
                    context?.openUri(R.string.url_changelog)
                },
                onRateTasks = {
                    viewModel.logEvent("rate_tasks")
                    context?.openUri(R.string.market_url)
                },
                onDocumentation = {
                    viewModel.logEvent("documentation")
                    context?.openUri(R.string.url_documentation)
                },
                onIssueTracker = {
                    viewModel.logEvent("issue_tracker")
                    context?.openUri(R.string.url_issue_tracker)
                },
                onContactDeveloper = {
                    viewModel.logEvent("contact_developer")
                    val uri = Uri.fromParts(
                        "mailto",
                        "Alex <${getString(R.string.support_email)}>",
                        null
                    )
                    val intent = Intent(Intent.ACTION_SENDTO, uri)
                        .putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback")
                        .putExtra(Intent.EXTRA_TEXT, viewModel.debugInfo)
                    startActivity(intent)
                },
                onSendLogs = {
                    viewModel.logEvent("send_logs")
                    lifecycleScope.launch {
                        val file = FileProvider.getUriForFile(
                            requireContext(),
                            Constants.FILE_PROVIDER_AUTHORITY,
                            viewModel.getLogZipFile()
                        )
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("message/rfc822")
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                            .putExtra(Intent.EXTRA_SUBJECT, "Tasks logs")
                            .putExtra(Intent.EXTRA_TEXT, viewModel.debugInfo)
                            .putExtra(Intent.EXTRA_STREAM, file)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    }
                },
                onReddit = {
                    viewModel.logEvent("reddit")
                    context?.openUri(R.string.url_reddit)
                },
                onTwitter = {
                    viewModel.logEvent("twitter")
                    context?.openUri(R.string.url_twitter)
                },
                onSourceCode = {
                    viewModel.logEvent("source_code")
                    context?.openUri(R.string.url_source_code)
                },
                onThirdPartyLicenses = {
                    viewModel.logEvent("third_party_licenses")
                    val intent = Intent()
                        .setClassName(requireContext(), "com.google.android.gms.oss.licenses.OssLicensesMenuActivity")
                    startActivity(intent)
                },
                onTermsOfService = {
                    viewModel.logEvent("tos")
                    context?.openUri(R.string.url_tos)
                },
                onPrivacyPolicy = {
                    viewModel.logEvent("privacy_policy")
                    context?.openUri(R.string.url_privacy_policy)
                },
                onCollectStatisticsChanged = { enabled ->
                    viewModel.updateCollectStatistics(enabled)
                },
            )

            if (viewModel.showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRestartDialog() },
                    text = { Text(stringResource(R.string.restart_required)) },
                    confirmButton = {
                        TextButton(onClick = { kotlin.system.exitProcess(0) }) {
                            Text(stringResource(R.string.restart_now))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissRestartDialog() }) {
                            Text(stringResource(R.string.restart_later))
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }
}
