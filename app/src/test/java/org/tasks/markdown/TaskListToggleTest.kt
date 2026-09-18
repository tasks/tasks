package org.tasks.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskListToggleTest {
    @Test
    fun checksFirstItem() {
        assertEquals(
            "- [x] a\n- [ ] b",
            toggleTaskListItem("- [ ] a\n- [ ] b", 0),
        )
    }

    @Test
    fun checksLastItem() {
        assertEquals(
            "- [ ] a\n- [x] b",
            toggleTaskListItem("- [ ] a\n- [ ] b", 1),
        )
    }

    @Test
    fun unchecksLowercaseX() {
        assertEquals("- [ ] a", toggleTaskListItem("- [x] a", 0))
    }

    @Test
    fun unchecksUppercaseX() {
        assertEquals("- [ ] a", toggleTaskListItem("- [X] a", 0))
    }

    @Test
    fun supportsAllBulletMarkers() {
        assertEquals(
            "* [x] a\n+ [x] b",
            toggleTaskListItem(toggleTaskListItem("* [ ] a\n+ [ ] b", 0)!!, 1),
        )
    }

    @Test
    fun supportsOrderedLists() {
        assertEquals(
            "1. [ ] a\n2) [x] b",
            toggleTaskListItem("1. [ ] a\n2) [ ] b", 1),
        )
    }

    @Test
    fun countsNestedItemsInDocumentOrder() {
        val source = "- [ ] parent\n  - [ ] child\n- [ ] sibling"
        assertEquals(
            "- [ ] parent\n  - [x] child\n- [ ] sibling",
            toggleTaskListItem(source, 1),
        )
    }

    @Test
    fun supportsBlockquotedItems() {
        assertEquals("> - [x] quoted", toggleTaskListItem("> - [ ] quoted", 0))
    }

    @Test
    fun ignoresItemsInsideBacktickFence() {
        val source = "```\n- [ ] code\n```\n- [ ] real"
        assertEquals(1, countTaskListItems(source))
        assertEquals("```\n- [ ] code\n```\n- [x] real", toggleTaskListItem(source, 0))
    }

    @Test
    fun ignoresItemsInsideTildeFence() {
        assertEquals(0, countTaskListItems("~~~\n- [ ] code\n~~~"))
    }

    @Test
    fun backtickFenceIsNotClosedByTildes() {
        // A fence only closes on the same character, so the "~~~" line is still code.
        assertEquals(0, countTaskListItems("```\n~~~\n- [ ] code"))
    }

    @Test
    fun ignoresBracketsMidLine() {
        assertEquals(0, countTaskListItems("see [ ] here\n- item [ ] not a task"))
    }

    @Test
    fun ignoresMarkerWithoutText() {
        // Markwon requires whitespace and content after the brackets, so "- [ ]" alone
        // renders as a plain bullet and must not shift the ordinals of later items.
        assertEquals(1, countTaskListItems("- [ ]\n- [ ]   \n- [ ] real"))
    }

    @Test
    fun ignoresMarkerWithoutSpaceBeforeText() {
        assertEquals(0, countTaskListItems("- [ ]text"))
    }

    @Test
    fun returnsNullForIndexOutOfRange() {
        assertNull(toggleTaskListItem("- [ ] a", 1))
        assertNull(toggleTaskListItem("- [ ] a", -1))
        assertNull(toggleTaskListItem("no tasks", 0))
    }

    @Test
    fun preservesEverythingButTheToggledCharacter() {
        val source = "# Title\r\n\r\n- [ ] **bold** item\r\n  more\r\ntrailing\n"
        assertEquals(
            "# Title\r\n\r\n- [x] **bold** item\r\n  more\r\ntrailing\n",
            toggleTaskListItem(source, 0),
        )
    }

    @Test
    fun togglesRenderedItemWhenCountsAgree() {
        assertEquals("- [ ] a\n- [x] b", toggleRenderedTaskListItem("- [ ] a\n- [ ] b", 1, 2))
    }

    @Test
    fun refusesToToggleWhenRendererSawDifferentItems() {
        // If Markwon rendered a different number of checkboxes than the scanner found, the
        // ordinals can't be trusted, so nothing may be rewritten.
        assertNull(toggleRenderedTaskListItem("- [ ] a\n- [ ] b", 1, 3))
        assertNull(toggleRenderedTaskListItem("- [ ] a\n- [ ] b", 0, 1))
    }

    @Test
    fun countsItems() {
        assertEquals(3, countTaskListItems("- [ ] a\n- [x] b\ntext\n1. [X] c"))
    }
}
