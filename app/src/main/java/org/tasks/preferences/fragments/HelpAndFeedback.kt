package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.extensions.Context.openUri
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.logging.FileLogger
import org.tasks.preferences.DiagnosticInfo
import javax.inject.Inject

@AndroidEntryPoint
class HelpAndFeedback : InjectingPreferenceFragment() {

    @Inject lateinit var firebase: Firebase
    @Inject lateinit var fileLogger: FileLogger
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var diagnosticInfo: DiagnosticInfo

    override fun getPreferenceXml() = R.xml.help_and_feedback

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.whats_new).summary =
            getString(R.string.version_string, BuildConfig.VERSION_NAME)
        openUrlWithEvent(R.string.whats_new, R.string.url_changelog, "whats_new")

        findPreference(R.string.contact_developer)
            .setOnPreferenceClickListener {
                firebase.logEvent(R.string.event_settings_click, R.string.param_type to "contact_developer")
                val uri = Uri.fromParts(
                    "mailto",
                    "Alex <" + getString(R.string.support_email) + ">",
                    null
                )
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback")
                    .putExtra(Intent.EXTRA_TEXT, diagnosticInfo.debugInfo)
                startActivity(intent)
                false
            }

        findPreference(R.string.send_application_logs)
            .setOnPreferenceClickListener {
                firebase.logEvent(R.string.event_settings_click, R.string.param_type to "send_logs")
                lifecycleScope.launch {
                    val file = FileProvider.getUriForFile(
                        requireContext(),
                        Constants.FILE_PROVIDER_AUTHORITY,
                        fileLogger.getZipFile()
                    )
                    val intent = Intent(Intent.ACTION_SEND)
                        .setType("message/rfc822")
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                        .putExtra(Intent.EXTRA_SUBJECT, "Tasks logs")
                        .putExtra(Intent.EXTRA_TEXT, diagnosticInfo.debugInfo)
                        .putExtra(Intent.EXTRA_STREAM, file)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                false
            }

        findPreference(R.string.p_collect_statistics)
            .setOnPreferenceClickListener {
                showRestartDialog()
                true
            }

        if (IS_GENERIC) {
            remove(
                R.string.p_collect_statistics,
                R.string.rate_tasks,
                R.string.third_party_licenses,
            )
        } else {
            findPreference(R.string.rate_tasks).setOnPreferenceClickListener {
                firebase.logEvent(R.string.event_settings_click, R.string.param_type to "rate_tasks")
                context?.openUri(R.string.market_url)
                false
            }
            findPreference(R.string.third_party_licenses).setOnPreferenceClickListener {
                firebase.logEvent(R.string.event_settings_click, R.string.param_type to "third_party_licenses")
                false
            }
        }

        openUrlWithEvent(R.string.documentation, R.string.url_documentation, "documentation")
        openUrlWithEvent(R.string.issue_tracker, R.string.url_issue_tracker, "issue_tracker")
        openUrlWithEvent(R.string.follow_reddit, R.string.url_reddit, "reddit")
        openUrlWithEvent(R.string.follow_twitter, R.string.url_twitter, "twitter")
        openUrlWithEvent(R.string.source_code, R.string.url_source_code, "source_code")
        if (IS_GOOGLE_PLAY || inventory.hasTasksAccount) {
            openUrlWithEvent(R.string.terms_of_service, R.string.url_tos, "tos")
        } else {
            remove(R.string.terms_of_service)
        }
        openUrlWithEvent(R.string.privacy_policy, R.string.url_privacy_policy, "privacy_policy")
    }

    private fun openUrlWithEvent(@StringRes prefId: Int, @StringRes url: Int, type: String) {
        findPreference(prefId).setOnPreferenceClickListener {
            firebase.logEvent(R.string.event_settings_click, R.string.param_type to type)
            context?.openUri(url)
            false
        }
    }

    override fun getMenu() = 0
}
