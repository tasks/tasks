package org.tasks.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.button.MaterialButton
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import org.tasks.preferences.Preferences
import javax.inject.Inject

class WhatsNewDialog : InjectingDialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var inventory: Inventory

    @BindView(R.id.changelog) lateinit var changelog: TextView
    @BindView(R.id.action_question) lateinit var actionQuestion: TextView
    @BindView(R.id.action_text) lateinit var actionText: TextView
    @BindView(R.id.action_button) lateinit var actionButton: MaterialButton
    @BindView(R.id.dismiss_button) lateinit var dismissButton: MaterialButton

    private var displayedRate = false
    private var displayedSubscribe = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_whats_new, null)
        ButterKnife.bind(this, view)

        val entries = resources.getStringArray(R.array.changelog).joinToString("\n\n") { "\u2022 $it" }
        val text = "$entries\n\nVisit https://tasks.org/changelog for more info"
        changelog.text = text

        val begForRating = !preferences.getBoolean(R.string.p_clicked_rate, false)
                && (inventory.purchasedThemes() || firebase.noChurn())
        val begForSubscription = firebase.noChurn() && !inventory.hasPro()

        when {
            BuildConfig.FLAVOR == "generic" -> {
                actionText.text = getString(R.string.upgrade_blurb_4)
                actionButton.text = getString(R.string.TLA_menu_donate)
                actionButton.setOnClickListener { onDonateClick() }
            }
            begForRating -> {
                displayedRate = true
                actionButton.text = getString(R.string.rate_tasks)
                actionButton.setOnClickListener { onRateClick() }
            }
            begForSubscription -> {
                displayedSubscribe = true
                actionText.text = getString(R.string.support_development_subscribe)
                actionButton.text = getString(R.string.button_subscribe)
                actionButton.setOnClickListener { onSubscribeClick() }
            }
            else -> {
                actionQuestion.visibility = View.GONE
                actionText.visibility = View.GONE
                actionButton.visibility = View.GONE
                dismissButton.text = getString(R.string.got_it)
            }
        }

        if (!resources.getBoolean(R.bool.whats_new_action)) {
            actionText.visibility = View.GONE
        }

        return dialogBuilder.newDialog(R.string.whats_new_in_version, BuildConfig.VERSION_NAME)
                .setView(view)
                .show()
    }

    private fun onSubscribeClick() {
        logClick(true)
        dismiss()
        startActivity(Intent(context, PurchaseActivity::class.java))
    }

    private fun onRateClick() {
        logClick(true)
        preferences.setBoolean(R.string.p_clicked_rate, true)
        dismiss()
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_url))))
    }

    private fun onDonateClick() {
        dismiss()
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tasks.org/donate")))
    }

    override fun onCancel(dialog: DialogInterface) {
        logClick(false)
        super.onCancel(dialog)
    }

    @OnClick(R.id.dismiss_button)
    fun onDismissClick() {
        logClick(false)
        dismiss()
    }

    private fun logClick(click: Boolean) {
        firebase.logEvent(
                R.string.event_whats_new,
                Pair(R.string.param_click, click),
                Pair(R.string.param_whats_new_display_rate, displayedRate),
                Pair(R.string.param_whats_new_display_subscribe, displayedSubscribe),
                Pair(R.string.param_user_pro, inventory.hasPro()),
                Pair(R.string.param_user_no_churn, firebase.noChurn()))
    }

    override fun inject(component: DialogFragmentComponent) = component.inject(this)
}