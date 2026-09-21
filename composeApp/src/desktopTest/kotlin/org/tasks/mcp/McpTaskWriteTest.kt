package org.tasks.mcp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.tasks.api.ListRow

class McpTaskWriteTest : McpGraphTestCase() {

    @Test
    fun createWithNoListsAtAllStillLands() = runBlocking {
        assertEquals(emptyList<ListRow>(), api.lists())

        val task = api.getTask(api.createTask(title = "First ever"))!!

        assertNotEquals(0L, task.listId)
        assertEquals(task.listId, api.lists().single().id)
    }
}
