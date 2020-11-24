package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.auth.AuthStateManager
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
import java.net.HttpURLConnection.HTTP_PAYMENT_REQUIRED
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : InjectingPreferenceFragment() {

    @Inject lateinit var authStateManager: AuthStateManager
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var toaster: Toaster

    lateinit var caldavAccount: CaldavAccount

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                if (inventory.subscription?.isTasksSubscription == true
                        && caldavAccount.error.isPaymentRequired()) {
                    caldavAccount.error = null
                    caldavDao.update(caldavAccount)
                }
                refreshUi()
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        caldavAccount = requireArguments().getParcelable(EXTRA_ACCOUNT)!!

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
            PurchaseDialog
                    .newPurchaseDialog(this, REQUEST_PURCHASE)
                    .show(parentFragmentManager, PurchaseDialog.FRAG_TAG_PURCHASE_DIALOG)
            false
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

        findPreference(R.string.sign_in_with_google).setOnPreferenceClickListener {
            activity?.startActivityForResult(
                    Intent(activity, SignInActivity::class.java),
                    Synchronization.REQUEST_TASKS_ORG)
            false
        }

        refreshUi()
    }

    private fun removeAccount() = lifecycleScope.launch {
        taskDeleter.delete(caldavAccount)
        authStateManager.signOut()
        activity?.onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)
        localBroadcastManager.registerRefreshListReceiver(purchaseReceiver)

        refreshUi()
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private fun refreshUi() {
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            isVisible = caldavAccount.error.isLoggedOut()
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
            if (caldavAccount.error.isPaymentRequired()) {
                if (subscription == null) {
                    setTitle(R.string.upgrade_to_pro)
                    setSummary(R.string.your_subscription_expired)
                } else {
                    setTitle(R.string.manage_subscription)
                    setSummary(R.string.insufficient_subscription)
                }
            } else {
                title = getString(
                        if (subscription == null) {
                            R.string.upgrade_to_pro
                        } else {
                            R.string.manage_subscription
                        })
                summary = if (subscription == null) {
                    null
                } else {
                    val price = getString(
                            if (subscription.isMonthly) R.string.price_per_month else R.string.price_per_year,
                            subscription.subscriptionPrice!! - .01
                    )
                    getString(R.string.current_subscription, price)
                }
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

        private fun String?.isLoggedOut(): Boolean =
                this?.startsWith("HTTP $HTTP_UNAUTHORIZED") == true

        private fun String?.isPaymentRequired(): Boolean =
                this?.startsWith("HTTP $HTTP_PAYMENT_REQUIRED") == true
    }
}