package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseDialog
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.jobs.WorkManager
import org.tasks.preferences.IconPreference
import org.tasks.ui.Toaster
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : InjectingPreferenceFragment() {

    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var toaster: Toaster

    private lateinit var caldavAccountLiveData: LiveData<CaldavAccount>

    val caldavAccount: CaldavAccount
        get() = caldavAccountLiveData.value ?: requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                caldavAccount.let {
                    if (inventory.subscription?.isTasksSubscription == true
                            && it.isPaymentRequired()) {
                        it.error = null
                        caldavDao.update(it)
                    }
                }
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        caldavAccountLiveData = caldavDao.watchAccount(
                requireArguments().getParcelable<CaldavAccount>(EXTRA_ACCOUNT)!!.id
        )

        findPreference(R.string.logout).setOnPreferenceClickListener {
            dialogBuilder
                    .newDialog()
                    .setMessage(R.string.logout_warning, getString(R.string.tasks_org))
                    .setPositiveButton(R.string.remove) { _, _ -> removeAccount() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            false
        }

        findPreference(R.string.upgrade_to_pro).setOnPreferenceClickListener {
            showPurchaseDialog()
        }

        findPreference(R.string.button_unsubscribe).setOnPreferenceClickListener {
            inventory.unsubscribe(requireActivity())
        }

        findPreference(R.string.refresh_purchases).setOnPreferenceClickListener {
            billingClient.queryPurchases()
            false
        }

        findPreference(R.string.offline_lists).setOnPreferenceClickListener {
            workManager.migrateLocalTasks(caldavAccount)
            toaster.longToast(R.string.migrating_tasks)
            false
        }

        if (isGitHubAccount) {
            findPreference(R.string.upgrade_to_pro).isVisible = false
            findPreference(R.string.button_unsubscribe).isVisible = false
            findPreference(R.string.refresh_purchases).isVisible = false
        }

        caldavAccountLiveData.observe(this) { account ->
            account?.let { refreshUi(it) }
        }
    }

    private fun showPurchaseDialog(): Boolean {
        PurchaseDialog
                .newPurchaseDialog(this, REQUEST_PURCHASE)
                .show(parentFragmentManager, PurchaseDialog.FRAG_TAG_PURCHASE_DIALOG)
        return false
    }

    private fun removeAccount() = lifecycleScope.launch {
        // try to delete session from caldav.tasks.org
        taskDeleter.delete(caldavAccount)
        inventory.updateTasksSubscription()
        activity?.onBackPressed()
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

    private val isGitHubAccount: Boolean
        get() = caldavAccount.username?.startsWith("github") == true

    private fun refreshUi(account: CaldavAccount) {
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            if (account.error.isNullOrBlank()) {
                isVisible = false
                return
            }
            isVisible = true
            when {
                account.isPaymentRequired() -> {
                    val subscription = inventory.subscription
                    if (isGitHubAccount) {
                        title = null
                        setSummary(R.string.insufficient_sponsorship)
                        if (BuildConfig.FLAVOR == "googleplay") {
                            onPreferenceClickListener = null
                        } else {
                            setOnPreferenceClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_sponsor))))
                                false
                            }
                        }
                    } else {
                        setOnPreferenceClickListener {
                            showPurchaseDialog()
                        }
                        if (subscription == null || subscription.isTasksSubscription) {
                            setTitle(R.string.button_subscribe)
                            setSummary(R.string.your_subscription_expired)
                        } else {
                            setTitle(R.string.manage_subscription)
                            setSummary(R.string.insufficient_subscription)
                        }
                    }
                }
                account.isLoggedOut() -> {
                    setTitle(if (isGitHubAccount) {
                        R.string.sign_in_with_github
                    } else {
                        R.string.sign_in_with_google
                    })
                    setSummary(R.string.authentication_required)
                    setOnPreferenceClickListener {
                        activity?.startActivityForResult(
                                Intent(activity, SignInActivity::class.java)
                                        .putExtra(
                                                SignInActivity.EXTRA_SELECT_SERVICE,
                                                if (isGitHubAccount) 1 else 0
                                        ),
                                Synchronization.REQUEST_TASKS_ORG)
                        false
                    }
                }
                else -> {
                    this.title = null
                    this.summary = account.error
                    this.onPreferenceClickListener = null
                }
            }
            iconVisible = true
        }

        lifecycleScope.launch {
            val listCount = caldavDao.listCount(CaldavDao.LOCAL)
            val quantityString = resources.getQuantityString(R.plurals.list_count, listCount, listCount)
            findPreference(R.string.migrate).isVisible = listCount > 0
            findPreference(R.string.offline_lists).summary =
                    getString(R.string.migrate_count, quantityString)
        }

        if (BuildConfig.FLAVOR == "generic") {
            return
        }
        val subscription = inventory.subscription
        findPreference(R.string.upgrade_to_pro).apply {
            title = getString(
                    if (subscription == null) {
                        R.string.button_subscribe
                    } else {
                        R.string.manage_subscription
                    }
            )
            summary = if (subscription == null) {
                null
            } else {
                val price = getString(
                        if (subscription.isMonthly) R.string.price_per_month else R.string.price_per_year,
                        (subscription.subscriptionPrice!! - .01).toString()
                )
                getString(R.string.current_subscription, price)
            }
        }
        findPreference(R.string.button_unsubscribe).isEnabled = inventory.subscription != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PURCHASE) {
            if (resultCode == RESULT_OK) {
                billingClient.queryPurchases()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val REQUEST_PURCHASE = 10201
        private const val EXTRA_ACCOUNT = "extra_account"

        fun newTasksAccountPreference(account: CaldavAccount): Fragment {
            val fragment = TasksAccount()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}