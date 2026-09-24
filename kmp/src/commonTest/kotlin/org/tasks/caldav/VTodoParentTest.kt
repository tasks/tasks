package org.tasks.caldav

import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.icalendar.RelatedTo
import org.tasks.icalendar.VTodo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VTodoParentTest {
    @Test
    fun rewritesTheExistingParentInPlace() {
        val todo = VTodo(
            relatedTo = mutableListOf(
                RelatedTo("sibling-uid", "SIBLING"),
                RelatedTo("old-parent", "PARENT", listOf("X-VENDOR" to "1")),
                RelatedTo("child-uid", "CHILD"),
            ),
        )

        todo.parent = "new-parent"

        assertEquals(
            listOf(
                RelatedTo("sibling-uid", "SIBLING"),
                RelatedTo("new-parent", "PARENT", listOf("X-VENDOR" to "1")),
                RelatedTo("child-uid", "CHILD"),
            ),
            todo.relatedTo,
        )
    }

    @Test
    fun collapsesDuplicateParents() {
        val todo = VTodo(
            relatedTo = mutableListOf(
                RelatedTo("one", "PARENT"),
                RelatedTo("two"),
                RelatedTo("child-uid", "CHILD"),
            ),
        )

        todo.parent = "three"

        assertEquals(
            listOf(RelatedTo("three", "PARENT"), RelatedTo("child-uid", "CHILD")),
            todo.relatedTo,
        )
    }

    @Test
    fun addsAParentWhenThereIsNone() {
        val todo = VTodo(relatedTo = mutableListOf(RelatedTo("child-uid", "CHILD")))

        todo.parent = "new-parent"

        assertEquals(
            listOf(RelatedTo("child-uid", "CHILD"), RelatedTo("new-parent")),
            todo.relatedTo,
        )
    }

    @Test
    fun clearingRemovesEveryParent() {
        val todo = VTodo(
            relatedTo = mutableListOf(
                RelatedTo("one", "PARENT"),
                RelatedTo("two"),
                RelatedTo("child-uid", "CHILD"),
            ),
        )

        todo.parent = null

        assertNull(todo.parent)
        assertEquals(listOf(RelatedTo("child-uid", "CHILD")), todo.relatedTo)
    }
}
