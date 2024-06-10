package org.tasks.compose.edit

import android.content.res.Configuration
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import com.todoroo.andlib.utility.AndroidUtilities
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.dialogs.Linkify
import org.tasks.markdown.MarkdownProvider
import org.tasks.themes.TasksTheme

@Composable
fun DescriptionRow(
    text: String?,
    onChanged: (CharSequence?) -> Unit,
    linkify: Linkify?,
    markdownProvider: MarkdownProvider?,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_notes_24px,
        content = {
            Column(verticalArrangement = Arrangement.Center) {
                Spacer(modifier = Modifier.height(11.dp))
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(end = 16.dp),
                    factory = { context ->
                        EditText(context).apply {
                            setText(text)
                            val textWatcher =
                                markdownProvider?.markdown(linkify != null)?.textWatcher(this)
                            addTextChangedListener(
                                onTextChanged = { text, _, _, _ -> onChanged(text) },
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
                            if (AndroidUtilities.atLeastOreo()) {
                                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                            }
                            isVerticalScrollBarEnabled = true
                            freezesText = true
                            setHorizontallyScrolling(false)
                            isHorizontalScrollBarEnabled = false
                            setHint(R.string.TEA_note_label)
                            setHintTextColor(context.getColor(R.color.text_tertiary))
                            setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                context.resources.getDimension(R.dimen.task_edit_text_size)
                            )
                            linkify?.linkify(this)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(11.dp))
            }
        },
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun EmptyDescriptionPreview() {
    TasksTheme {
        DescriptionRow(
            text = null,
            onChanged = {},
            linkify = null,
            markdownProvider = null,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun DescriptionPreview() {
    TasksTheme {
        DescriptionRow(
            text = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

                Eleifend quam adipiscing vitae proin sagittis. Faucibus a pellentesque sit amet porttitor eget dolor.
            """.trimIndent(),
            onChanged = {},
            linkify = null,
            markdownProvider = null,
        )
    }
}
