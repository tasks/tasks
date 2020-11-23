package org.tasks.billing

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.databinding.ActivityPurchaseBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.locale.Locale
import org.tasks.themes.Theme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseDialog : DialogFragment(), OnPurchasesUpdated {

    interface PurchaseHandler {
        fun onPurchaseDialogDismissed()
    }

    private val purchaseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setup()
        }
    }

    @Inject lateinit var tasksTheme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var locale: Locale

    private lateinit var binding: ActivityPurchaseBinding
    private lateinit var markwon: Markwon

    private var currentSubscription: Purchase? = null
    private var priceChanged = false
    private var nameYourPrice = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        ButterKnife.bind(this, binding.root)

        if (savedInstanceState != null) {
            binding.slider.value = savedInstanceState.getFloat(EXTRA_PRICE)
            priceChanged = savedInstanceState.getBoolean(EXTRA_PRICE_CHANGED)
            nameYourPrice = savedInstanceState.getBoolean(EXTRA_NAME_YOUR_PRICE)
        }

        binding.slider.addOnChangeListener(this::onPriceChanged)
        binding.slider.setLabelFormatter {
            "$${it - .01}"
        }
        binding.text.movementMethod = LinkMovementMethod.getInstance()

        markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .build()

        if (BuildConfig.FLAVOR != "generic") {
            setWaitScreen(true)
        } else {
            setWaitScreen(false)
            binding.payAnnually.isVisible = false
            binding.payMonthly.isVisible = false
            binding.payOther.isVisible = false
            binding.sponsor.isVisible = true
        }

        return dialogBuilder.newDialog()
                .setView(binding.root)
                .show()
    }

    private fun updateText() {
        var benefits = "### ${getString(R.string.upgrade_header)}"
        benefits += if (nameYourPrice) {
            """
---
#### ~~${getString(R.string.upgrade_sync_with_tasks)}~~
"""
        } else {
            """
---                
#### [${getString(R.string.upgrade_sync_with_tasks)} (BETA)](${getString(R.string.help_url_sync)})
* **${getString(R.string.upgrade_no_platform_lock_in)}** — ${getString(R.string.upgrade_open_internet_standards)}
* **${getString(R.string.upgrade_customer)}** — ${getString(R.string.upgrade_privacy)}
* ${getString(R.string.upgrade_coming_soon)}
"""
        }
        benefits += if (BuildConfig.FLAVOR == "generic") {
            """
---
**${getString(R.string.upgrade_previous_donors)}** - [${getString(R.string.contact_developer)}](mailto:${getString(R.string.support_email)}) ${getString(R.string.upgrade_previous_donors_contact)}
"""
        } else {
            """
---
#### ${getString(R.string.upgrade_synchronization)}
* [${getString(R.string.davx5)}](${getString(R.string.url_davx5)})
* [${getString(R.string.caldav)}](${getString(R.string.url_caldav)})
* [${getString(R.string.upgrade_etesync)}](${getString(R.string.url_etesync)})
* ${getString(R.string.upgrade_google_tasks)}
---
#### ${getString(R.string.upgrade_additional_features)}
* ${getString(R.string.upgrade_themes)}
* ${getString(R.string.upgrade_google_places)}
* [${getString(R.string.upgrade_tasker)}](${getString(R.string.url_tasker)})
---
* ${getString(R.string.upgrade_free_trial)}
* **${getString(R.string.upgrade_downgrade)}** — ${getString(R.string.upgrade_balance)}
* **${getString(R.string.upgrade_cancel)}** — ${getString(R.string.upgrade_benefits_retained)}
"""
        }
        binding.text.text = markwon.toMarkdown(benefits)
    }

    @OnClick(R.id.pay_annually)
    fun subscribeAnnually() {
        initiatePurchase(false, 30)
    }
    
    @OnClick(R.id.pay_monthly)
    fun subscribeMonthly() {
        initiatePurchase(true, 3)
    }

    @OnClick(R.id.sponsor)
    fun sponsor() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_sponsor))))
    }
    
    private fun initiatePurchase(isMonthly: Boolean, price: Int) {
        val newSku = String.format("%s_%02d", if (isMonthly) "monthly" else "annual", price)
        billingClient.initiatePurchaseFlow(
                requireActivity(),
                newSku,
                BillingClientImpl.TYPE_SUBS,
                currentSubscription?.sku?.takeIf { it != newSku })
        billingClient.addPurchaseCallback(this)
    }
    
    @OnClick(R.id.pay_other)
    fun nameYourPrice() {
        nameYourPrice = !nameYourPrice
        setWaitScreen(false)
        binding.scroll.scrollTo(0, 0)
        updateSubscribeButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(EXTRA_PRICE, binding.slider.value)
        outState.putBoolean(EXTRA_PRICE_CHANGED, priceChanged)
        outState.putBoolean(EXTRA_NAME_YOUR_PRICE, nameYourPrice)
    }

    private fun setWaitScreen(isWaitScreen: Boolean) {
        Timber.d("setWaitScreen(%s)", isWaitScreen)
        binding.slider.isVisible = !isWaitScreen && nameYourPrice
        binding.payOther.isVisible = !isWaitScreen
        binding.payOther.setText(if (nameYourPrice) R.string.back else R.string.more_options)
        binding.tasksOrgButtonPanel.isVisible = !isWaitScreen
        binding.screenWait.isVisible = isWaitScreen
        updateText()
    }

    override fun onStart() {
        super.onStart()
        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)
        billingClient.queryPurchases()
    }

    override fun onStop() {
        super.onStop()
        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private fun setup() {
        currentSubscription = inventory.subscription
        if (!priceChanged) {
            binding.slider.value =
                    currentSubscription
                            ?.subscriptionPrice
                            ?.coerceAtMost(25)
                            ?.toFloat() ?: 10f
        }
        updateSubscribeButton()
        setWaitScreen(false)
    }

    private fun updateSubscribeButton() {
        val sliderValue = binding.slider.value.toInt()
        val annualPrice = if (nameYourPrice) sliderValue else 30
        val monthlyPrice = if (nameYourPrice) sliderValue else 3
        val constrained = resources.getBoolean(R.bool.width_constrained)
        binding.payAnnually.let {
            it.isEnabled = true
            it.text = getString(
                    if (constrained) R.string.price_per_year_abbreviated else R.string.price_per_year,
                    annualPrice - .01
            )
            it.setOnClickListener {
                initiatePurchase(false, if (nameYourPrice) sliderValue else 30)
            }
        }
        binding.payMonthly.let {
            it.isEnabled = true
            it.text = getString(
                    if (constrained) R.string.price_per_month_abbreviated else R.string.price_per_month,
                    monthlyPrice - .01
            )
            it.setOnClickListener {
                initiatePurchase(true, if (nameYourPrice) sliderValue else 3)
            }
            it.isVisible = !nameYourPrice || sliderValue < 3
        }
        currentSubscription?.let {
            binding.payMonthly.isEnabled =
                    it.isCanceled || !it.isMonthly || monthlyPrice != it.subscriptionPrice
            binding.payAnnually.isEnabled =
                    it.isCanceled || it.isMonthly || annualPrice != it.subscriptionPrice
        }
    }

    private fun onPriceChanged(slider: Slider, value: Float, fromUser: Boolean) {
        if (fromUser) {
            priceChanged = true
        }
        updateSubscribeButton()
    }

    override fun onPurchasesUpdated(success: Boolean) {
        if (success) {
            dismiss()
            targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        activity.takeIf { it is PurchaseHandler }?.let {
            (it as PurchaseHandler).onPurchaseDialogDismissed()
        }
        if (arguments?.getBoolean(EXTRA_FINISH_ACTIVITY, false) == true) {
            activity?.finish()
        }
    }

    companion object {
        private const val EXTRA_PRICE = "extra_price"
        private const val EXTRA_PRICE_CHANGED = "extra_price_changed"
        private const val EXTRA_NAME_YOUR_PRICE = "extra_name_your_price"
        const val EXTRA_FINISH_ACTIVITY = "extra_activity_rc"
        @JvmStatic
        val FRAG_TAG_PURCHASE_DIALOG = "frag_tag_purchase_dialog"

        @JvmStatic
        fun newPurchaseDialog() = newPurchaseDialog(false)

        fun newPurchaseDialog(finishActivity: Boolean): PurchaseDialog {
            val dialog = PurchaseDialog()
            val args = Bundle()
            args.putBoolean(EXTRA_FINISH_ACTIVITY, finishActivity)
            dialog.arguments = args
            return dialog
        }

        fun newPurchaseDialog(target: Fragment, rc: Int): PurchaseDialog {
            val dialog = PurchaseDialog()
            dialog.setTargetFragment(target, rc)
            return dialog
        }
    }
}