package org.tasks.ui

import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditViewModel.Companion.stripCarriageReturns
import javax.inject.Inject

@AndroidEntryPoint
class DescriptionControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var markdownProvider: MarkdownProvider

    private val linkifyEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_linkify_task_edit, false)

    @Composable
    override fun Body() {
        Column(verticalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.height(11.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(end = 16.dp),
                factory = { context ->
                    EditText(context).apply {
                        setText(viewModel.description.stripCarriageReturns())
                        val textWatcher =
                            markdownProvider.markdown(linkifyEnabled).textWatcher(this)
                        addTextChangedListener(
                            onTextChanged = { text, _, _, _ -> textChanged(text) },
                            afterTextChanged = { editable -> textWatcher?.invoke(editable) }
                        )
                        setBackgroundColor(context.getColor(android.R.color.transparent))
                        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                        inputType =
                            InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                        isSingleLine = false
                        maxLines = Int.MAX_VALUE
                        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                        isVerticalScrollBarEnabled = true
                        freezesText = true
                        setHorizontallyScrolling(false)
                        isHorizontalScrollBarEnabled = false
                        setHint(R.string.TEA_note_label)
                        setHintTextColor(context.getColor(R.color.text_tertiary))
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.task_edit_text_size))
                        if (linkifyEnabled) {
                            linkify.linkify(this)
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(11.dp))
        }
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