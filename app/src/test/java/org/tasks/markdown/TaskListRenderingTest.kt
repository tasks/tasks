package org.tasks.markdown

import android.text.Spanned
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Checks the assumption [toggleTaskListItem] relies on: its scanner finds the same task
 * items, in the same order, as the checkboxes Markwon actually renders.
 */
@RunWith(RobolectricTestRunner::class)
class TaskListRenderingTest {
    private val markwon = Markwon(ApplicationProvider.getApplicationContext(), false)

    private fun render(source: String) = markwon.toMarkdown(source) as Spanned

    private fun assertScannerMatchesRenderer(source: String) {
        val spans = taskListSpans(render(source))
        assertEquals(source, countTaskListItems(source), spans.size)
        spans.forEachIndexed { index, span ->
            // The toggle changes exactly one character: the one between this item's brackets.
            val toggled = toggleTaskListItem(source, index)!!
            val marker = source.indices.single { source[it] != toggled[it] }
            assertEquals("item $index of: $source", source[marker] in "xX", span.isDone)
        }
    }

    @Test
    fun matchesSimpleList() = assertScannerMatchesRenderer("- [ ] a\n- [x] b\n- [X] c")

    @Test
    fun matchesOrderedAndMixedBullets() =
        assertScannerMatchesRenderer("1. [ ] a\n2. [x] b\n\n* [ ] c\n+ [x] d")

    @Test
    fun matchesNestedItems() =
        assertScannerMatchesRenderer("- [ ] parent\n  - [x] child\n  - [ ] child 2\n- [ ] sibling")

    @Test
    fun matchesWithFencedCode() =
        assertScannerMatchesRenderer("```\n- [ ] code\n```\n- [ ] real\n~~~\n- [x] more code\n~~~")

    @Test
    fun matchesBlockquotedItems() = assertScannerMatchesRenderer("> - [ ] quoted\n> - [x] done")

    @Test
    fun matchesFormattingAfterMarker() =
        assertScannerMatchesRenderer("- [ ] **bold** and [a link](https://tasks.org)")

    @Test
    fun matchesItemsWithoutText() = assertScannerMatchesRenderer("- [ ]\n- [ ]text\n- [ ] real")

    @Test
    fun matchesAMixedDocument() = assertScannerMatchesRenderer(
        """
        # Shopping

        Some *intro* text with [ ] brackets.

        - [ ] milk
        - [x] bread
          continued line

        | a | b |
        |---|---|
        | 1 | 2 |

        1. [ ] call
        """.trimIndent()
    )

    @Test
    fun findsCheckboxByTheLineItStartsOn() {
        val text = render("- [ ] parent\n  - [ ] child")
        val spans = taskListSpans(text)
        assertEquals(0, taskListIndexAt(text, text.getSpanStart(spans[0])))
        assertEquals(1, taskListIndexAt(text, text.getSpanStart(spans[1])))
    }

    @Test
    fun noCheckboxOnPlainLine() {
        val text = render("plain\n\n- [ ] task")
        assertNull(taskListIndexAt(text, 0))
    }
}
