package org.tasks.billing

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.button.MaterialButtonToggleGroup
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.databinding.ActivityPurchaseBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.IconLayoutManager
import org.tasks.injection.ActivityComponent
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.locale.Locale
import timber.log.Timber
import javax.inject.Inject

private const val EXTRA_MONTHLY = "extra_monthly"
private const val EXTRA_PRICE = "extra_price"

class PurchaseActivity : ThemedInjectingAppCompatActivity(), OnPurchasesUpdated, Toolbar.OnMenuItemClickListener {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var locale: Locale

    private lateinit var binding: ActivityPurchaseBinding
    private lateinit var adapter: PurchaseAdapter

    private var currentSubscription: Purchase? = null

    private val purchaseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ButterKnife.bind(this)

        adapter = PurchaseAdapter(this, tasksTheme, locale, ::onPriceChanged)

        if (savedInstanceState != null) {
            binding.buttons.check(
                    if (savedInstanceState.getBoolean(EXTRA_MONTHLY)) R.id.button_monthly else R.id.button_annually)
            adapter.selected = savedInstanceState.getInt(EXTRA_PRICE)

        }

        binding.buttons.addOnButtonCheckedListener { group: MaterialButtonToggleGroup?, id: Int, checked: Boolean -> this.onButtonChecked(group!!, id, checked) }

        val toolbar = binding.toolbar.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationContentDescription(R.string.back)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setTitle(R.string.upgrade_to_pro)
        toolbar.inflateMenu(R.menu.menu_purchase_activity)
        toolbar.setOnMenuItemClickListener(this)

        themeColor.apply(toolbar)

        setWaitScreen(true)
    }

    @SuppressLint("DefaultLocale")
    @OnClick(R.id.subscribe)
    fun subscribe() {
        if (currentSubscriptionSelected() && currentSubscription?.isCanceled == true) {
            billingClient.initiatePurchaseFlow(
                    this, currentSubscription!!.sku, BillingClientImpl.TYPE_SUBS, null)
        } else {
            billingClient.initiatePurchaseFlow(this, String.format("%s_%02d", if (isMonthly()) "monthly" else "annual", adapter.selected),
                    BillingClientImpl.TYPE_SUBS,
                    currentSubscription?.sku)
        }
        billingClient.addPurchaseCallback(this)
    }

    private fun onButtonChecked(group: MaterialButtonToggleGroup, id: Int, checked: Boolean) {
        if (id == R.id.button_monthly) {
            if (!checked && group.checkedButtonId != R.id.button_annually) {
                group.check(R.id.button_monthly)
            }
        } else {
            if (!checked && group.checkedButtonId != R.id.button_monthly) {
                group.check(R.id.button_annually)
            }
        }
        updateSubscribeButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_MONTHLY, isMonthly())
        outState.putInt(EXTRA_PRICE, adapter.selected)
    }

    private fun isMonthly() = binding.buttons.checkedButtonId == R.id.button_monthly

    private fun setWaitScreen(isWaitScreen: Boolean) {
        Timber.d("setWaitScreen(%s)", isWaitScreen)
        binding.recyclerView.visibility = if (isWaitScreen) View.GONE else View.VISIBLE
        binding.buttons.visibility = if (isWaitScreen) View.GONE else View.VISIBLE
        binding.subscribe.visibility = if (isWaitScreen) View.GONE else View.VISIBLE
        binding.screenWait.visibility = if (isWaitScreen) View.VISIBLE else View.GONE
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

    override fun inject(component: ActivityComponent) {
        component.inject(this)
    }

    private fun setup() {
        currentSubscription = inventory.subscription
        if (adapter.selected == 0) {
            adapter.selected = currentSubscription?.subscriptionPrice ?: 3
            if (currentSubscription != null) {
                binding.buttons.check(if (currentSubscription?.isMonthly == true) R.id.button_monthly else R.id.button_annually)
            }
        }
        binding.unsubscribe.visibility = if (currentSubscription == null || currentSubscription?.isCanceled == true) View.GONE else View.VISIBLE
        updateSubscribeButton()
        setWaitScreen(false)
        adapter.submitList((1..10).toList())
        binding.recyclerView.layoutManager = IconLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun updateSubscribeButton() {
        binding.subscribe.isEnabled = true
        if (currentSubscription == null) {
            binding.subscribe.setText(R.string.button_subscribe)
        } else if (currentSubscriptionSelected()) {
            if (currentSubscription!!.isCanceled) {
                binding.subscribe.setText(R.string.button_restore_subscription)
            } else {
                binding.subscribe.setText(R.string.button_current_subscription)
                binding.subscribe.isEnabled = false
            }
        } else {
            binding.subscribe.setText(if (isUpgrade()) R.string.button_upgrade else R.string.button_downgrade)
        }
    }

    private fun currentSubscriptionSelected() =
            currentSubscription != null
                    && isMonthly() == currentSubscription!!.isMonthly
                    && adapter.selected == currentSubscription!!.subscriptionPrice

    private fun isUpgrade() = if (isMonthly() == currentSubscription!!.isMonthly) {
        currentSubscription!!.subscriptionPrice!! < adapter.selected
    } else {
        isMonthly()
    }

    private fun onPriceChanged(price: Int) {
        adapter.selected = price
        updateSubscribeButton()
    }

    @OnClick(R.id.unsubscribe)
    fun manageSubscription() {
        startActivity(
                Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.manage_subscription_url, currentSubscription!!.sku))))
    }

    override fun onPurchasesUpdated(success: Boolean) {
        if (success) {
            finish()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.menu_more_info) {
            startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.subscription_help_url))))
            true
        } else {
            false
        }
    }
}