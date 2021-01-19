package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.injection.InjectingPreferenceFragment

private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"

@AndroidEntryPoint
class HelpAndFeedback : InjectingPreferenceFragment() {

    override fun getPreferenceXml() = R.xml.help_and_feedback

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        val whatsNew = findPreference(R.string.whats_new)
        whatsNew.summary = getString(R.string.version_string, BuildConfig.VERSION_NAME)
        whatsNew.setOnPreferenceClickListener {
            val fragmentManager: FragmentManager = parentFragmentManager
            if (fragmentManager.findFragmentByTag(FRAG_TAG_WHATS_NEW) == null) {
                WhatsNewDialog().show(fragmentManager, FRAG_TAG_WHATS_NEW)
            }
            true
        }

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

        findPreference(R.string.p_collect_statistics)
            .setOnPreferenceClickListener {
                showRestartDialog()
                true
            }

        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR == "generic") {
            remove(
                R.string.p_collect_statistics,
                R.string.rate_tasks,
            )
        }
    }

    override fun getMenu() = 0
}