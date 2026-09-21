package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.api.TasksContract.Tasks

class TasksContractTest {

    @Test
    fun aTaskIdIsReadOutOfItsOwnItemUri() {
        assertEquals(42L, Tasks.idIn("content://org.tasks.api/v0/tasks/42"))
        assertNull(Tasks.idIn("content://org.tasks.api/v0/tasks"))
        assertNull(Tasks.idIn("content://org.tasks.api/v0/tasks/42/extra"))
        assertNull(Tasks.idIn("content://org.tasks.api/v0/tasks/abc"))
        assertNull(Tasks.idIn("content://org.tasks.api/v0/reminders/42"))
        assertNull(Tasks.idIn("content://org.tasks/tasks/42"))
    }
}
