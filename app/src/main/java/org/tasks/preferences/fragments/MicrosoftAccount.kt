package org.tasks.preferences.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.IconPreference
import org.tasks.sync.microsoft.MicrosoftSignInViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MicrosoftAccount : BaseAccountPreference() {

    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var caldavDao: CaldavDao

    private val microsoftVM: MicrosoftSignInViewModel by viewModels()
    private lateinit var microsoftAccountLiveData: LiveData<CaldavAccount>

    val microsoftAccount: CaldavAccount
        get() = microsoftAccountLiveData.value ?: requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                microsoftAccount.let {
                    if (inventory.subscription.value != null && it.error.isPaymentRequired()) {
                        it.error = null
                        caldavDao.update(it)
                    }
                    refreshUi(it)
                }
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_google_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        super.setupPreferences(savedInstanceState)

        microsoftAccountLiveData = caldavDao.watchAccount(
                arguments?.getParcelable<CaldavAccount>(EXTRA_ACCOUNT)?.id ?: 0
        )
        microsoftAccountLiveData.observe(this) { refreshUi(it) }

        findPreference(R.string.reinitialize_account)
                .setOnPreferenceClickListener { requestLogin() }
    }

    override suspend fun removeAccount() {
        taskDeleter.delete(microsoftAccount)
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)
        localBroadcastManager.registerRefreshListReceiver(purchaseReceiver)
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private fun refreshUi(account: CaldavAccount?) {
        if (account == null) {
            return
        }
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            if (account.error.isNullOrBlank()) {
                isVisible = false
                return@apply
            }
            isVisible = true
            when {
                account.error.isPaymentRequired() -> {
                    setOnPreferenceClickListener { showPurchaseDialog() }
                    setTitle(R.string.name_your_price)
                    setSummary(R.string.requires_pro_subscription)
                }
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
        microsoftAccount.username?.let {
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