package org.tasks.markdown

import android.text.Layout
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.ext.tasklist.TaskListSpan

/**
 * All checkboxes rendered into [text], in document order.
 *
 * Markwon draws a task-list checkbox as a [TaskListSpan], which is a leading-margin span
 * covering the item's text rather than a character of its own, so it can't be made
 * clickable with a ClickableSpan. Callers hit-test the margin instead.
 */
fun taskListSpans(text: Spanned): List<TaskListSpan> =
    text.getSpans(0, text.length, TaskListSpan::class.java).sortedBy { text.getSpanStart(it) }

/**
 * The ordinal of the checkbox belonging to the task item that starts at [lineStart].
 *
 * A span also covers any items nested inside it, but it only draws its checkbox on the
 * item's own first line, so it is matched by its start offset.
 *
 * @param text the rendered Markdown
 * @param lineStart the offset of the first character on the tapped line
 * @return the checkbox's zero-based position in document order, or null if no task item
 *   starts on that line
 */
fun taskListIndexAt(text: Spanned, lineStart: Int): Int? =
    taskListSpans(text)
        .indexOfFirst { text.getSpanStart(it) == lineStart }
        .takeIf { it >= 0 }

/**
 * The ordinal of the task-list checkbox drawn under a touch at ([x], [y]), in view
 * coordinates, or null if the touch isn't on a checkbox.
 *
 * A checkbox occupies its span's leading margin on the item's first line, which is the
 * strip immediately before where that line's text begins.
 */
fun TextView.taskListCheckboxAt(x: Float, y: Float): Int? {
    val layout: Layout = layout ?: return null
    val text = text as? Spanned ?: return null
    val localY = (y - totalPaddingTop + scrollY).toInt()
    if (localY < 0 || localY >= layout.height) {
        return null
    }
    val line = layout.getLineForVertical(localY)
    val lineStart = layout.getLineStart(line)
    val index = taskListIndexAt(text, lineStart) ?: return null
    val margin = taskListSpans(text)[index].getLeadingMargin(true)
    val localX = x - totalPaddingLeft + scrollX
    // getPrimaryHorizontal includes the leading margins, so it is where the text starts.
    val textStart = layout.getPrimaryHorizontal(lineStart)
    val onCheckbox = if (layout.getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT) {
        localX > textStart && localX <= textStart + margin
    } else {
        localX < textStart && localX >= textStart - margin
    }
    return index.takeIf { onCheckbox }
}
