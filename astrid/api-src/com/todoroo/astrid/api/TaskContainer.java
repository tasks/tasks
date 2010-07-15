package com.todoroo.astrid.api;

import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Container class for tasks. Synchronization Providers can subclass
 * this class if desired.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskContainer {
    public Task task;
    public Metadata[] metadata;

    public TaskContainer(Task task, Metadata[] metadata) {
        this.task = task;
        this.metadata = metadata;
    }
}