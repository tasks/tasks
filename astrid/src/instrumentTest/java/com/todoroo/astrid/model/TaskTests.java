/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.model;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;

public class TaskTests extends DatabaseTestCase {

    @Autowired
    TaskService taskService;

    /** Check task gets a creation date at some point */
    public void testCreationDate() {
        Task task = new Task();
        taskService.save(task);
        assertTrue(task.getValue(Task.CREATION_DATE) > 0);
    }
}
