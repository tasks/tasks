package org.tasks.preferences.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import javax.inject.Inject

class HelpAndFeedback : InjectingPreferenceFragment() {

    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var inventory: Inventory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.help_and_feedback, rootKey)

        findPreference(R.string.changelog).summary =
            getString(R.string.version_string, BuildConfig.VERSION_NAME)

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

        findPreference(R.string.refresh_purchases)
            .setOnPreferenceClickListener {
                billingClient.queryPurchases()
                false
            }

        findPreference(R.string.p_collect_statistics)
            .setOnPreferenceClickListener {
                showRestartDialog()
                true
            }

        if (inventory.hasPro()) {
            val findPreference = findPreference(R.string.upgrade_to_pro)
            findPreference.title = getString(R.string.manage_subscription)
            findPreference.summary = getString(R.string.manage_subscription_summary)
        }

        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR == "generic") {
            remove(
                R.string.p_collect_statistics,
                R.string.rate_tasks,
                R.string.upgrade_to_pro,
                R.string.refresh_purchases
            )
        }
    }

    override fun inject(component: FragmentComponent) {
        component.inject(this);
    }
}