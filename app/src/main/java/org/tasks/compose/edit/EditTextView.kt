package org.tasks.compose.edit

import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Paint
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import org.tasks.R
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider

@Composable
fun EditTextView(
    text: String?,
    hint: String?,
    onChanged: (CharSequence?) -> Unit,
    linkify: Linkify?,
    markdownProvider: MarkdownProvider?,
    strikethrough: Boolean = false,
    requestFocus: Boolean = false,
    multiline: Boolean = false,
    onDone: () -> Unit = {},
) {
    val context = LocalContext.current
    var shouldRequestFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(end = 16.dp)
            .focusRequester(focusRequester),
        factory = { context ->
            EditText(context).apply {
                setText(text)
                val textWatcher =
                    markdownProvider?.markdown(linkify != null)?.textWatcher(this)
                addTextChangedListener(
                    onTextChanged = { text, _, _, _ -> onChanged(text) },
                    afterTextChanged = { editable -> textWatcher?.invoke(editable) }
                )
                maxLines = Int.MAX_VALUE
                isSingleLine = false
                if (multiline) {
                    // Multiline configuration
                    setRawInputType(
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    )
                    imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                } else {
                    // Single line with Done button
                    setRawInputType(
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                                InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    )
                    imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                    setImeActionLabel(context.getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE)
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            clearFocus()
                            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(windowToken, 0)
                            onDone()
                            true
                        } else {
                            false
                        }
                    }
                }

                setBackgroundColor(context.getColor(android.R.color.transparent))
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                freezesText = true
                setHorizontallyScrolling(false)
                setHint(hint)
                setHintTextColor(context.getColor(R.color.text_tertiary))
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.task_edit_text_size)
                )
                linkify?.linkify(this)
            }
        },
        update = { view ->
            if (shouldRequestFocus) {
                view.requestFocus()
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                shouldRequestFocus = false
            }
            view.paintFlags = if (strikethrough) {
                view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                view.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        },
    )

    LaunchedEffect(Unit) {
        if (requestFocus) {
            shouldRequestFocus = true
        }
    }
}