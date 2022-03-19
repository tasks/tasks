package org.tasks.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.databinding.DialogWhatsNewBinding
import org.tasks.extensions.Context.openUri
import org.tasks.markdown.MarkdownProvider
import java.io.BufferedReader
import javax.inject.Inject

@AndroidEntryPoint
class WhatsNewDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var markdownProvider: MarkdownProvider

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogWhatsNewBinding.inflate(layoutInflater)

        val textStream = requireContext().assets.open("CHANGELOG.md")
        val text = BufferedReader(textStream.reader()).readText()
        binding.changelog.movementMethod = LinkMovementMethod.getInstance()
        markdownProvider
            .markdown(linkify = true, force = true)
            .setMarkdown(binding.changelog, text)

        binding.dismissButton.setOnClickListener {
            dismiss()
        }

        return dialogBuilder.newDialog()
                .setView(binding.root)
                .show()
    }

    private fun onSubscribeClick() {
        dismiss()
        startActivity(Intent(context, PurchaseActivity::class.java))
    }

    private fun onDonateClick() {
        dismiss()
        context?.openUri(R.string.url_donate)
    }
}