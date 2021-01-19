package org.tasks.preferences.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseDialog.Companion.FRAG_TAG_PURCHASE_DIALOG
import org.tasks.billing.PurchaseDialog.Companion.newPurchaseDialog
import org.tasks.dialogs.WhatsNewDialog
import org.tasks.injection.InjectingPreferenceFragment
import javax.inject.Inject

private const val FRAG_TAG_WHATS_NEW = "frag_tag_whats_new"

@AndroidEntryPoint
class HelpAndFeedback : InjectingPreferenceFragment() {

    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshSubscription()
        }
    }

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

        findPreference(R.string.refresh_purchases).setOnPreferenceClickListener {
            billingClient.queryPurchases()
            false
        }

        findPreference(R.string.p_collect_statistics)
            .setOnPreferenceClickListener {
                showRestartDialog()
                true
            }

        findPreference(R.string.button_unsubscribe).setOnPreferenceClickListener {
            inventory.unsubscribe(requireActivity())
        }

        findPreference(R.string.upgrade_to_pro).setOnPreferenceClickListener {
            newPurchaseDialog().show(parentFragmentManager, FRAG_TAG_PURCHASE_DIALOG)
            false
        }

        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR == "generic") {
            remove(
                R.string.p_collect_statistics,
                R.string.rate_tasks,
                R.string.upgrade_to_pro,
                R.string.button_unsubscribe,
                R.string.refresh_purchases
            )
        }
    }

    override fun onResume() {
        super.onResume()

        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)

        refreshSubscription()
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private fun refreshSubscription() {
        if (BuildConfig.FLAVOR == "generic") {
            return
        }

        val subscription = inventory.subscription.value
        findPreference(R.string.upgrade_to_pro).apply {
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
                        (subscription.subscriptionPrice!! - .01).toString()
                )
                getString(R.string.current_subscription, price)
            }
        }
        findPreference(R.string.button_unsubscribe).isEnabled = inventory.subscription.value != null
    }

    override fun getMenu() = 0
}