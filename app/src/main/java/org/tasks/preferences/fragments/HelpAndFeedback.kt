package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.TasksUrls
import org.tasks.compose.settings.HelpAndFeedbackContent
import org.tasks.extensions.Context.openUri
import org.tasks.logging.FileLogger
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.DiagnosticInfo
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class HelpAndFeedback : Fragment() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var diagnosticInfo: DiagnosticInfo
    @Inject lateinit var fileLogger: FileLogger
    @Inject lateinit var preferences: Preferences

    private val viewModel: HelpAndFeedbackHiltViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            HelpAndFeedbackContent(
                viewModel = viewModel,
                openUri = { context?.openUri(it) },
                onRestartApplication = { exitProcess(0) },
                onRateTasks = {
                    context?.openUri(R.string.market_url)
                },
                onContactDeveloper = {
                    val uri = Uri.fromParts(
                        "mailto",
                        "Alex <${TasksUrls.SUPPORT_EMAIL}>",
                        null
                    )
                    val intent = Intent(Intent.ACTION_SENDTO, uri)
                        .putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback")
                        .putExtra(Intent.EXTRA_TEXT, diagnosticInfo.debugInfo)
                    startActivity(intent)
                },
                onSendLogs = {
                    lifecycleScope.launch {
                        val file = FileProvider.getUriForFile(
                            requireContext(),
                            Constants.FILE_PROVIDER_AUTHORITY,
                            fileLogger.getZipFile()
                        )
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("message/rfc822")
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf(TasksUrls.SUPPORT_EMAIL))
                            .putExtra(Intent.EXTRA_SUBJECT, "Tasks logs")
                            .putExtra(Intent.EXTRA_TEXT, diagnosticInfo.debugInfo)
                            .putExtra(Intent.EXTRA_STREAM, file)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    }
                },
                onThirdPartyLicenses = {
                    val intent = Intent()
                        .setClassName(requireContext(), "com.google.android.gms.oss.licenses.OssLicensesMenuActivity")
                    startActivity(intent)
                },
                onCollectStatisticsChanged = { enabled ->
                    preferences.setBoolean(R.string.p_collect_statistics, enabled)
                },
                bottomInsets = {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                },
            )
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
