package org.tasks.preferences.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskDeleter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.PurchaseActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.InjectingPreferenceFragment
import javax.inject.Inject

abstract class BaseAccountPreference : InjectingPreferenceFragment() {

    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDeleter: TaskDeleter

    private val accountState = MutableStateFlow<CaldavAccount?>(null)

    val account: CaldavAccount
        get() = accountState.value ?: requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        caldavDao
            .watchAccount(requireArguments().getParcelable<CaldavAccount>(EXTRA_ACCOUNT)!!.id)
            .onEach {
                accountState.value = it
                if (it != null) {
                    refreshUi(it)
                }
            }
            .launchIn(lifecycleScope)

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

    protected open suspend fun removeAccount() {
        taskDeleter.delete(account)
    }

    protected abstract suspend fun refreshUi(account: CaldavAccount)

    protected fun showPurchaseDialog(): Boolean {
        startActivityForResult(
            Intent(context, PurchaseActivity::class.java),
            REQUEST_PURCHASE
        )
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PURCHASE) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    billingClient.queryPurchases()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        @JvmStatic
        protected val EXTRA_ACCOUNT = "extra_account"
        const val REQUEST_PURCHASE = 10201
    }
}