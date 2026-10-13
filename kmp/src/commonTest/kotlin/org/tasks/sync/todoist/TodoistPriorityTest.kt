package org.tasks.sync.todoist

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.sync.todoist.TodoistConverter.applyRemote
import org.tasks.sync.todoist.TodoistConverter.toArgs
import org.tasks.sync.todoist.TodoistConverter.toDueDate
import org.tasks.sync.todoist.TodoistConverter.toTaskPriority
import org.tasks.sync.todoist.TodoistConverter.toTodoistPriority
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoistPriorityTest {    @Test
    fun priorityMappingIsABijection() {
        assertEquals(Task.Priority.HIGH, 4.toTaskPriority())
        assertEquals(Task.Priority.MEDIUM, 3.toTaskPriority())
        assertEquals(Task.Priority.LOW, 2.toTaskPriority())
        assertEquals(Task.Priority.NONE, 1.toTaskPriority())

        assertEquals(4, Task.Priority.HIGH.toTodoistPriority())
        assertEquals(3, Task.Priority.MEDIUM.toTodoistPriority())
        assertEquals(2, Task.Priority.LOW.toTodoistPriority())
        assertEquals(1, Task.Priority.NONE.toTodoistPriority())
    }    @Test
    fun priorityMappingClampsOutOfRangeInput() {
        assertEquals(Task.Priority.HIGH, 9.toTaskPriority())
        assertEquals(Task.Priority.NONE, 0.toTaskPriority())
        assertEquals(Task.Priority.NONE, (-5).toTaskPriority())
    }
}
