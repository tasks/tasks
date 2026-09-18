package org.tasks.markdown

/*
 * Maps a rendered task-list checkbox back to its "[ ]" marker in the Markdown source.
 *
 * Markwon 4.6.2 is built on commonmark-java 0.13, which records no source positions, so a
 * rendered checkbox can only be identified by its ordinal. This scanner walks the source
 * with the same rule Markwon's TaskListPostProcessor applies - a list item whose text starts
 * with "[ ]", "[x]" or "[X]" followed by whitespace and content - and yields the markers in
 * document order. Callers must compare [countTaskListItems] with the number of checkboxes
 * actually rendered and refuse to toggle on a mismatch, so a construct this scanner reads
 * differently from Markwon can never rewrite the wrong line.
 */

// Optional blockquote prefixes and indentation, a bullet or ordered-list marker, then the
// bracket. The lookahead mirrors Markwon's "\s+(.*)" after the trailing whitespace of a
// paragraph has been stripped: at least one space, then something that isn't whitespace.
private val TASK_MARKER = Regex("""^((?:[ \t]*>)*[ \t]*(?:[-*+]|\d{1,9}[.)])[ \t]+)\[[ \txX]](?=[ \t]+\S)""")

private val FENCE = Regex("""^(?:[ \t]*>)*[ \t]*(`{3,}|~{3,})""")

private fun offsetsInsideTaskListBrackets(source: String): List<Int> {
    val offsets = mutableListOf<Int>()
    var fence: String? = null
    var lineStart = 0
    while (lineStart <= source.length) {
        val lineEnd = source.indexOf('\n', lineStart).let { if (it < 0) source.length else it }
        val line = source.substring(lineStart, lineEnd)
        val fenceMatch = FENCE.find(line)?.groupValues?.get(1)
        when {
            fence != null -> {
                // A fence closes only on the same character, at least as long as it opened.
                if (fenceMatch != null && fenceMatch[0] == fence[0] && fenceMatch.length >= fence.length) {
                    fence = null
                }
            }
            fenceMatch != null -> fence = fenceMatch
            else -> TASK_MARKER.find(line)?.let {
                offsets.add(lineStart + it.groupValues[1].length + 1)
            }
        }
        lineStart = lineEnd + 1
    }
    return offsets
}

/**
 * Counts the task-list items Markwon will render as checkboxes.
 *
 * @param source the Markdown source
 * @return the number of task-list markers outside fenced code blocks
 */
fun countTaskListItems(source: String): Int = offsetsInsideTaskListBrackets(source).size

/**
 * Flips the [index]-th task-list item between checked and unchecked.
 *
 * Only the single character inside the brackets changes, so line endings, indentation and
 * the rest of the text are preserved byte for byte.
 *
 * @param source the Markdown source
 * @param index zero-based position of the checkbox in document order
 * @return the updated source, or null if there is no task-list item at [index]
 */
fun toggleTaskListItem(source: String, index: Int): String? {
    val offset = offsetsInsideTaskListBrackets(source).getOrNull(index) ?: return null
    val replacement = if (source[offset] == 'x' || source[offset] == 'X') ' ' else 'x'
    return StringBuilder(source).apply { setCharAt(offset, replacement) }.toString()
}

/**
 * Toggles the checkbox a user tapped in rendered Markdown, but only when the renderer and
 * this scanner agree on how many task items there are.
 *
 * @param source the Markdown source that was rendered
 * @param index zero-based position of the tapped checkbox among the rendered ones
 * @param renderedCount how many checkboxes the renderer produced
 * @return the updated source, or null if the tap can't be mapped safely
 */
fun toggleRenderedTaskListItem(source: String, index: Int, renderedCount: Int): String? =
    if (countTaskListItems(source) == renderedCount) toggleTaskListItem(source, index) else null
