package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.IconPreference
import javax.inject.Inject

@AndroidEntryPoint
class GoogleTasksAccount : BaseAccountPreference() {

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun getPreferenceXml() = R.xml.preferences_google_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        super.setupPreferences(savedInstanceState)

        findPreference(R.string.reinitialize_account)
                .setOnPreferenceClickListener { requestLogin() }
    }

    override suspend fun refreshUi(account: CaldavAccount) {
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            if (account.error.isNullOrBlank()) {
                isVisible = false
                return@apply
            }
            isVisible = true
            when {
                account.error.isUnauthorized() -> {
                    setTitle(R.string.sign_in_with_google)
                    setSummary(R.string.authentication_required)
                    setOnPreferenceClickListener { requestLogin() }
                }
                else -> {
                    this.title = null
                    this.summary = account.error
                    this.onPreferenceClickListener = null
                }
            }
            iconVisible = true
        }
    }

    private fun requestLogin(): Boolean {
        activity?.startActivityForResult(
                Intent(activity, GtasksLoginActivity::class.java),
                MainSettingsFragment.REQUEST_GOOGLE_TASKS
        )
        return false
    }

    companion object {
        fun String?.isUnauthorized(): Boolean =
                this?.startsWith("401 Unauthorized", ignoreCase = true) == true

        fun newGoogleTasksAccountPreference(account: CaldavAccount) =
                GoogleTasksAccount().apply {
                    arguments = Bundle().apply {
                        putParcelable(EXTRA_ACCOUNT, account)
                    }
                }
    }
}