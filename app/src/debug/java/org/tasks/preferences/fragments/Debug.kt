package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.preference.Preference
import at.bitfire.cert4android.CustomCertManager.Companion.resetCertificates
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.ui.Toaster
import javax.inject.Inject

class Debug : InjectingPreferenceFragment() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var toaster: Toaster

    override fun getPreferenceXml() = R.xml.preferences_debug

    override fun setupPreferences(savedInstanceState: Bundle?) {
        for (pref in listOf(
            R.string.p_leakcanary,
            R.string.p_flipper,
            R.string.p_strict_mode_vm,
            R.string.p_strict_mode_thread
        )) {
            findPreference(pref)
                .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                    showRestartDialog()
                    true
                }
        }

        findPreference(R.string.debug_reset_ssl).setOnPreferenceClickListener {
            resetCertificates(requireContext())
            toaster.longToast("SSL certificates reset")
            false
        }

        setupIap(R.string.debug_themes, Inventory.SKU_THEMES)
        setupIap(R.string.debug_tasker, Inventory.SKU_TASKER)
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

    override fun inject(component: FragmentComponent) = component.inject(this)
}