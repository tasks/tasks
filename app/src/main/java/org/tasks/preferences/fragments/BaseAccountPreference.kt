package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.PurchaseDialog
import org.tasks.injection.InjectingPreferenceFragment
import javax.inject.Inject

abstract class BaseAccountPreference : InjectingPreferenceFragment() {

    @Inject lateinit var billingClient: BillingClient

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.logout).setOnPreferenceClickListener {
            dialogBuilder
                    .newDialog()
                    .setMessage(R.string.logout_warning)
                    .setPositiveButton(R.string.remove) { _, _ ->
                        lifecycleScope.launch {
                            withContext(NonCancellable) {
                                removeAccount()
                            }
                            activity?.onBackPressed()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            false
        }
    }

    protected abstract suspend fun removeAccount()

    protected fun showPurchaseDialog(tasksPayment: Boolean = false): Boolean {
        PurchaseDialog
                .newPurchaseDialog(this, REQUEST_PURCHASE, tasksPayment)
                .show(parentFragmentManager, PurchaseDialog.FRAG_TAG_PURCHASE_DIALOG)
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PURCHASE) {
            if (resultCode == Activity.RESULT_OK) {
                billingClient.queryPurchases()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val REQUEST_PURCHASE = 10201

    }
}