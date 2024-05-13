package org.tasks.preferences.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.preferences.IconPreference
import javax.inject.Inject

@AndroidEntryPoint
class GoogleTasksAccount : BaseAccountPreference() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                if (inventory.subscription.value != null && account.error.isPaymentRequired()) {
                    caldavDao.update(account.copy(error = null))
                }
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_google_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        super.setupPreferences(savedInstanceState)

        findPreference(R.string.reinitialize_account)
                .setOnPreferenceClickListener { requestLogin() }
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

    override suspend fun refreshUi(account: CaldavAccount) {
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