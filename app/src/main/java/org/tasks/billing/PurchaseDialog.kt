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
import android.text.style.TextAppearanceSpan
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.databinding.ActivityPurchaseBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.locale.Locale
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

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var locale: Locale
    @Inject lateinit var firebase: Firebase

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
        binding.slider.setLabelFormatter { "$${it - .01}" }
        binding.text.movementMethod = LinkMovementMethod.getInstance()

        markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                        builder.appendFactory(Strikethrough::class.java) { _, _ ->
                            TextAppearanceSpan(requireContext(), R.style.RedText)
                        }
                    }
                })
                .build()

        setWaitScreen(BuildConfig.FLAVOR != "generic")

        return if (BuildConfig.FLAVOR == "generic") {
            if (isGitHub) {
                getPurchaseDialog()
            } else {
                getMessageDialog(R.string.no_google_play_subscription)
            }
        } else {
            if (isGitHub) {
                getMessageDialog(R.string.insufficient_sponsorship)
            } else {
                getPurchaseDialog()
            }
        }
    }

    private fun getPurchaseDialog(): AlertDialog =
            dialogBuilder.newDialog().setView(binding.root).show()

    private fun getMessageDialog(res: Int): AlertDialog =
            dialogBuilder.newDialog()
                    .setMessage(res)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.help) { _, _ ->
                        val url = Uri.parse(getString(R.string.subscription_help_url))
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                    }
                    .show()

    private fun updateText() {
        var benefits = "### ${getString(when {
            nameYourPrice -> R.string.name_your_price
            !inventory.hasPro -> R.string.upgrade_to_pro
            !inventory.hasTasksSubscription -> R.string.button_upgrade
            else -> R.string.manage_subscription
        })}"
        benefits += if (nameYourPrice) {
            """
---
#### ~~${getString(R.string.upgrade_tasks_account)}~~

_${getString(R.string.upgrade_tasks_no_account)}_
"""
        } else {
            """
---                
#### ${getString(R.string.upgrade_tasks_account)}
* ${getString(R.string.tasks_org_description)}
* [${getString(R.string.upgrade_third_party_apps)}](${getString(R.string.url_app_passwords)})
* [${getString(R.string.upgrade_coming_soon)}](${getString(R.string.help_url_sync)})
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
#### ${getString(R.string.upgrade_sync_self_hosted)}
* [${getString(R.string.davx5)}](${getString(R.string.url_davx5)})
* [${getString(R.string.caldav)}](${getString(R.string.url_caldav)})
* [${getString(R.string.etesync)}](${getString(R.string.url_etesync)})
* [${getString(R.string.decsync)}](${getString(R.string.url_decsync)})
* ${getString(R.string.upgrade_google_tasks)}
---
#### ${getString(R.string.upgrade_additional_features)}
* ${getString(R.string.upgrade_themes)}
* ${getString(R.string.upgrade_google_places)}
* [${getString(R.string.upgrade_tasker)}](${getString(R.string.url_tasker)})
---
* ${getString(R.string.upgrade_free_trial)}
* ${getString(R.string.upgrade_downgrade)}
* ${getString(R.string.upgrade_support_development)}
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
        val generic = BuildConfig.FLAVOR == "generic"
        binding.sliderContainer.isVisible = !isWaitScreen && nameYourPrice
        binding.payOther.isVisible = !isWaitScreen
        if (nameYourPrice) {
            binding.payOther.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            binding.payOther.setText(R.string.back)
        } else {
            binding.payOther.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_keyboard_arrow_right_24px, 0)
            binding.payOther.setText(R.string.more_options)
        }
        binding.tasksOrgButtonPanel.isVisible = !isWaitScreen && !generic
        binding.screenWait.isVisible = isWaitScreen && !generic
        binding.sponsor.isVisible = generic
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
        val aboveAverage = "${getString(R.string.above_average)} $POPPER"
        binding.avgAnnual.text = when {
            !nameYourPrice -> "${getString(
                    R.string.save_percent,
                    ((1 - (annualPrice / (12.0 * monthlyPrice))) * 100).toInt()
            )} $POPPER"
            sliderValue < firebase.averageSubscription() -> "" //getString(R.string.below_average)
            else -> aboveAverage
        }
        binding.avgAnnual.setTextColor(
                if (nameYourPrice && sliderValue < firebase.averageSubscription()) {
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.purchase_highlight)
                }
        )
        binding.avgMonthly.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.purchase_highlight)
        )
        binding.avgMonthly.text = aboveAverage
        with(binding.payAnnually) {
            isEnabled = true
            text = getString(
                if (constrained) R.string.price_per_year_abbreviated else R.string.price_per_year,
                annualPrice - .01
            )
            setOnClickListener {
                initiatePurchase(false, if (nameYourPrice) sliderValue else 30)
            }
        }
        with(binding.payMonthly) {
            isEnabled = true
            text = getString(
                if (constrained) R.string.price_per_month_abbreviated else R.string.price_per_month,
                monthlyPrice - .01
        )
            setOnClickListener {
                initiatePurchase(true, if (nameYourPrice) sliderValue else 3)
            }
            isVisible = !nameYourPrice || sliderValue < 3
        }

        binding.avgMonthly.isVisible = nameYourPrice && binding.payMonthly.isVisible
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
    }

    private val isTasksPayment: Boolean
        get() = arguments?.getBoolean(EXTRA_TASKS_PAYMENT, false) ?: false

    private val isGitHub: Boolean
        get() = arguments?.getBoolean(EXTRA_GITHUB, false) ?: false

    companion object {
        private const val POPPER = "\uD83C\uDF89"
        private const val EXTRA_PRICE = "extra_price"
        private const val EXTRA_PRICE_CHANGED = "extra_price_changed"
        private const val EXTRA_NAME_YOUR_PRICE = "extra_name_your_price"
        private const val EXTRA_TASKS_PAYMENT = "extra_tasks_payment"
        private const val EXTRA_GITHUB = "extra_github"

        @JvmStatic
        val FRAG_TAG_PURCHASE_DIALOG = "frag_tag_purchase_dialog"

        @JvmStatic
        @JvmOverloads
        fun newPurchaseDialog(
                tasksPayment: Boolean = false,
                github: Boolean = BuildConfig.FLAVOR == "generic"
        ): PurchaseDialog {
            val dialog = PurchaseDialog()
            val args = Bundle()
            args.putBoolean(EXTRA_TASKS_PAYMENT, tasksPayment)
            args.putBoolean(EXTRA_GITHUB, github)
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