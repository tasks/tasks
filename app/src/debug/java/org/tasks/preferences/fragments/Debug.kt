package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import at.bitfire.cert4android.CustomCertManager.Companion.resetCertificates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.extensions.Context.toast
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class Debug : InjectingPreferenceFragment() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var preferences: Preferences

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

        findPreference(R.string.debug_clear_hints).setOnPreferenceClickListener {
            preferences.installDate =
                min(preferences.installDate, currentTimeMillis() - TimeUnit.DAYS.toMillis(14))
            preferences.lastSubscribeRequest = 0L
            preferences.lastReviewRequest = 0L
            preferences.shownBeastModeHint = false
            true
        }
    }

    private fun setupIap(@StringRes prefId: Int, sku: String) {
        val preference: Preference = findPreference(prefId)
        if (inventory.getPurchase(sku) == null) {
            preference.title = getString(R.string.debug_purchase, sku)
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                lifecycleScope.launch {
                    billingClient.initiatePurchaseFlow(requireActivity().parent, "inapp" /*SkuType.INAPP*/, sku)
                }
                false
            }
        } else {
            preference.title = getString(R.string.debug_consume, sku)
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                lifecycleScope.launch {
                    billingClient.consume(sku)
                }
                false
            }
        }
    }
}