package org.tasks.compose.edit

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PointF
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import org.jetbrains.compose.resources.stringResource as composeStringResource
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.dialogs.Linkify
import org.tasks.markdown.Markdown
import org.tasks.markdown.MarkdownProvider
import org.tasks.markdown.Markwon
import org.tasks.markdown.taskListCheckboxAt
import org.tasks.markdown.taskListSpans
import org.tasks.markdown.toggleRenderedTaskListItem
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.edit_description

/**
 * The task description. With Markdown enabled, a non-empty description is shown rendered;
 * tapping it switches to the raw text for editing, and it renders again once the field loses
 * focus. Tapping a rendered checkbox toggles it in place without leaving the preview.
 */
@Composable
fun DescriptionRow(
    text: String?,
    onChanged: (CharSequence?) -> Unit,
    linkify: Linkify?,
    markdownProvider: MarkdownProvider?,
) {
    val autoLink = linkify != null
    val markdown = remember(markdownProvider, autoLink) { markdownProvider?.markdown(autoLink) }
    // Saveable so a rotation mid-edit comes back to the editor, not the preview.
    var editing by rememberSaveable { mutableStateOf(false) }
    val preview = text?.takeIf { markdown?.enabled == true && !editing && it.isNotBlank() }
    TaskEditRow(
        iconRes = R.drawable.ic_outline_notes_24px,
        content = {
            Column(verticalArrangement = Arrangement.Center) {
                Spacer(modifier = Modifier.height(11.dp))
                if (preview != null && markdown != null) {
                    MarkdownPreview(
                        text = preview,
                        markdown = markdown,
                        linkify = linkify,
                        onEdit = { editing = true },
                        onToggle = { index, renderedCount ->
                            // A tap that can't be mapped to exactly one marker must never
                            // rewrite the wrong line, so fall back to editing the source.
                            toggleRenderedTaskListItem(preview, index, renderedCount)
                                ?.let(onChanged)
                                ?: run { editing = true }
                        },
                    )
                } else {
                    EditTextView(
                        text = text,
                        hint = stringResource(R.string.TEA_note_label),
                        onChanged = onChanged,
                        linkify = linkify,
                        markdownProvider = markdownProvider,
                        multiline = true,
                        requestFocus = editing,
                        cursorAtEnd = true,
                        onFocusChanged = { editing = it },
                    )
                }
                Spacer(modifier = Modifier.height(11.dp))
            }
        },
    )
}

/**
 * Read-only Markdown rendering of the description, laid out to match [EditTextView] so that
 * switching between the two doesn't move the text.
 *
 * @param onEdit called for a tap anywhere other than a link or a checkbox
 * @param onToggle called with the tapped checkbox's ordinal and the number of checkboxes
 *   rendered, so the caller can verify the mapping before changing the source
 */
@SuppressLint("ClickableViewAccessibility") // the touch listener only records, never consumes
@Composable
private fun MarkdownPreview(
    text: String,
    markdown: Markdown,
    linkify: Linkify?,
    onEdit: () -> Unit,
    onToggle: (index: Int, renderedCount: Int) -> Unit,
) {
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val editLabel = composeStringResource(Res.string.edit_description)
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(end = 16.dp),
        factory = { context ->
            TextView(context).apply {
                // EditTextView keeps the EditText's own padding after swapping its background
                // for a transparent one, so take the metrics from an identical EditText.
                val reference = EditText(context).apply {
                    setBackgroundColor(context.getColor(android.R.color.transparent))
                }
                setPadding(
                    reference.paddingLeft,
                    reference.paddingTop,
                    reference.paddingRight,
                    reference.paddingBottom,
                )
                minimumHeight = reference.minimumHeight
                setTextColor(reference.textColors)
                setLinkTextColor(reference.linkTextColors)
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.task_edit_text_size)
                )
            }
        },
        update = { view ->
            ViewCompat.replaceAccessibilityAction(
                view,
                AccessibilityActionCompat.ACTION_CLICK,
                editLabel,
                null,
            )
            val rendered = text to markdown
            if (view.tag == rendered) {
                return@AndroidView
            }
            view.tag = rendered
            markdown.setMarkdown(view, text)
            // A click carries no position, so remember where the finger lifted to tell a
            // checkbox tap from a tap on the text. An accessibility click has no touch at all
            // and leaves this null, which means "edit".
            var lastTap: PointF? = null
            view.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    lastTap = PointF(event.x, event.y)
                }
                false
            }
            val onTap = {
                val index = lastTap?.let { view.taskListCheckboxAt(it.x, it.y) }
                lastTap = null
                if (index != null) {
                    currentOnToggle(index, taskListSpans(view.text as Spanned).size)
                } else {
                    currentOnEdit()
                }
            }
            if (linkify != null) {
                // Sends link taps to the "open or edit" dialog, which always offers a way into
                // the editor, and everything else to onTap.
                view.movementMethod = LinkMovementMethod.getInstance()
                linkify.setMovementMethod(view, rowClickHandler = onTap)
            } else {
                // Links are off for the edit screen, so they must not open on tap: a
                // description that is only a link would otherwise never reach the editor
                // (see #4423). Markwon installs a movement method after rendering, so clear
                // it afterwards; every tap then lands here.
                view.movementMethod = null
                view.setOnClickListener { onTap() }
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

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun RenderedDescriptionPreview() {
    val context = LocalContext.current
    TasksTheme {
        MarkdownPreview(
            text = """
                ## Groceries
                - [x] **Milk**
                - [ ] Bread, see [the list](https://tasks.org)
            """.trimIndent(),
            markdown = remember { Markwon(context, false) },
            linkify = null,
            onEdit = {},
            onToggle = { _, _ -> },
        )
    }
}
