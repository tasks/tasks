package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GENERIC
import org.tasks.analytics.Firebase
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.logging.FileLogger
import javax.inject.Inject

@AndroidEntryPoint
class HelpAndFeedback : InjectingPreferenceFragment() {

    @Inject lateinit var firebase: Firebase
    @Inject lateinit var fileLogger: FileLogger

    override fun getPreferenceXml() = R.xml.help_and_feedback

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.whats_new).summary =
            getString(R.string.version_string, BuildConfig.VERSION_NAME)
        openUrl(R.string.whats_new, R.string.url_changelog)

        findPreference(R.string.contact_developer)
            .setOnPreferenceClickListener {
                val uri = Uri.fromParts(
                    "mailto",
                    "Alex <" + getString(R.string.support_email) + ">",
                    null
                )
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "Tasks Feedback")
                    .putExtra(Intent.EXTRA_TEXT, device.debugInfo)
                startActivity(intent)
                false
            }

        findPreference(R.string.send_application_logs)
            .setOnPreferenceClickListener {
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
                        .putExtra(Intent.EXTRA_TEXT, device.debugInfo)
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
            openUrl(R.string.rate_tasks, R.string.market_url)
        }

        openUrl(R.string.documentation, R.string.url_documentation)
        openUrl(R.string.issue_tracker, R.string.url_issue_tracker)
        openUrl(R.string.follow_reddit, R.string.url_reddit)
        openUrl(R.string.follow_twitter, R.string.url_twitter)
        openUrl(R.string.source_code, R.string.url_source_code)
        openUrl(R.string.terms_of_service, R.string.url_tos)
        openUrl(R.string.privacy_policy, R.string.url_privacy_policy)
    }

    override fun getMenu() = 0
}