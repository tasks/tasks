package org.tasks.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.databinding.DialogWhatsNewBinding
import org.tasks.extensions.Context.openUri
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import java.io.BufferedReader
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class WhatsNewDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var markdownProvider: MarkdownProvider

    private var displayedRate = false
    private var displayedSubscribe = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogWhatsNewBinding.inflate(layoutInflater)

        val textStream = requireContext().assets.open("CHANGELOG.md")
        val text = BufferedReader(textStream.reader()).readText()
        binding.changelog.movementMethod = LinkMovementMethod.getInstance()
        markdownProvider.markdown(true).setMarkdown(binding.changelog, text)

        val begForSubscription = !inventory.hasPro
        val begForRating = !preferences.getBoolean(R.string.p_clicked_rate, false)
                && (inventory.purchasedThemes() || firebase.noChurn())
                && (!begForSubscription || Random.nextBoolean())

        when {
            IS_GENERIC -> {
                binding.actionQuestion.setText(R.string.enjoying_tasks)
                binding.actionText.setText(R.string.tasks_needs_your_support)
                binding.actionButton.text = getString(R.string.TLA_menu_donate)
                binding.actionButton.setOnClickListener { onDonateClick() }
            }
            begForRating -> {
                displayedRate = true
                binding.actionQuestion.setText(R.string.enjoying_tasks)
                binding.actionButton.setText(R.string.rate_tasks)
                binding.actionButton.setOnClickListener { onRateClick() }
            }
            begForSubscription -> {
                displayedSubscribe = true
                binding.actionQuestion.setText(R.string.tasks_needs_your_support)
                binding.actionText.setText(R.string.support_development_subscribe)
                binding.actionButton.setText(R.string.name_your_price)
                binding.actionButton.setOnClickListener { onSubscribeClick() }
            }
            else -> {
                binding.actionQuestion.visibility = View.GONE
                binding.actionText.visibility = View.GONE
                binding.actionButton.visibility = View.GONE
                binding.dismissButton.text = getString(R.string.got_it)
            }
        }

        if (!resources.getBoolean(R.bool.whats_new_action)) {
            binding.actionText.visibility = View.GONE
        }

        binding.dismissButton.setOnClickListener {
            logClick(false)
            dismiss()
        }

        return dialogBuilder.newDialog()
                .setView(binding.root)
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
        context?.openUri(R.string.market_url)
    }

    private fun onDonateClick() {
        dismiss()
        context?.openUri(R.string.url_donate)
    }

    override fun onCancel(dialog: DialogInterface) {
        logClick(false)
        super.onCancel(dialog)
    }

    private fun logClick(click: Boolean) {
        firebase.logEvent(
                R.string.event_whats_new,
                Pair(R.string.param_click, click),
                Pair(R.string.param_whats_new_display_rate, displayedRate),
                Pair(R.string.param_whats_new_display_subscribe, displayedSubscribe),
                Pair(R.string.param_user_pro, inventory.hasPro),
                Pair(R.string.param_user_no_churn, firebase.noChurn()))
    }
}