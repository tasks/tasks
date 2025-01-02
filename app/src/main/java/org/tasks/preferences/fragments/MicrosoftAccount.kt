package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.IconPreference
import org.tasks.sync.microsoft.MicrosoftSignInViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MicrosoftAccount : BaseAccountPreference() {

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val microsoftVM: MicrosoftSignInViewModel by viewModels()

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
                    setTitle(R.string.sign_in)
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
        account.username?.let {
            microsoftVM.signIn(requireActivity()) // should force a specific account
        }
        return false
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun String?.isUnauthorized(): Boolean =
                this?.startsWith("401 Unauthorized", ignoreCase = true) == true

        fun newMicrosoftAccountPreference(account: CaldavAccount) =
                MicrosoftAccount().apply {
                    arguments = Bundle().apply {
                        putParcelable(EXTRA_ACCOUNT, account)
                    }
                }
    }
}