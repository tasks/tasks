package org.tasks.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetDescriptionBinding
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class DescriptionControlSet : TaskEditControlViewFragment() {
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var markdownProvider: MarkdownProvider

    private lateinit var editText: EditText

    private val linkifyEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_linkify_task_edit, false)

    override fun createView(savedInstanceState: Bundle?) {
        viewModel.description?.let(editText::setTextKeepState)
        if (linkifyEnabled) {
            linkify.linkify(editText)
        }
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetDescriptionBinding.inflate(layoutInflater, parent, true).let {
            editText = it.notes
            val textWatcher = markdownProvider.markdown(linkifyEnabled).textWatcher(editText)
            editText.addTextChangedListener(
                onTextChanged = { text, _, _, _ -> textChanged(text) },
                afterTextChanged = { editable -> textWatcher?.invoke(editable) }
            )
            it.root
        }

    override val icon = R.drawable.ic_outline_notes_24px

    override fun controlId() = TAG

    private fun textChanged(text: CharSequence?) {
        viewModel.description = text?.toString()?.trim { it <= ' ' }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_notes_pref
    }
}