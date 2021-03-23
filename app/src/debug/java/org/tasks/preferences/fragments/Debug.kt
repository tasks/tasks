package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.preference.Preference
import at.bitfire.cert4android.CustomCertManager.Companion.resetCertificates
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.extensions.Context.toast
import org.tasks.injection.InjectingPreferenceFragment
import javax.inject.Inject

@AndroidEntryPoint
class Debug : InjectingPreferenceFragment() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var billingClient: BillingClient

    override fun getPreferenceXml() = R.xml.preferences_debug

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        for (pref in listOf(
            R.string.p_leakcanary,
            R.string.p_flipper,
            R.string.p_strict_mode_vm,
            R.string.p_strict_mode_thread,
            R.string.p_crash_main_queries
        )) {
            findPreference(pref)
                .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                    showRestartDialog()
                    true
                }
        }

        findPreference(R.string.debug_reset_ssl).setOnPreferenceClickListener {
            resetCertificates(requireContext())
            context?.toast("SSL certificates reset")
            false
        }

        findPreference(R.string.debug_force_restart).setOnPreferenceClickListener {
            restart()
            false
        }

        setupIap(R.string.debug_themes, Inventory.SKU_THEMES)
        setupIap(R.string.debug_tasker, Inventory.SKU_TASKER)

        findPreference(R.string.debug_crash_app).setOnPreferenceClickListener {
            throw RuntimeException("Crashed app from debug preferences")
        }
    }

    private fun setupIap(@StringRes prefId: Int, sku: String) {
        val preference: Preference = findPreference(prefId)
        if (inventory.getPurchase(sku) == null) {
            preference.title = getString(R.string.debug_purchase, sku)
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                billingClient.initiatePurchaseFlow(activity, sku, "inapp" /*SkuType.INAPP*/, null)
                false
            }
        } else {
            preference.title = getString(R.string.debug_consume, sku)
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                billingClient.consume(sku)
                false
            }
        }
    }
}