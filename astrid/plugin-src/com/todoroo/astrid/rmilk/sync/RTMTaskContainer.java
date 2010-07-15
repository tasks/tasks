package com.todoroo.astrid.rmilk.sync;

import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

public class RTMTaskContainer extends TaskContainer {
    public String listId, taskSeriesId, taskId;

    public RTMTaskContainer(Task task, Metadata[] metadata,
            String listId, String taskSeriesId, String taskId) {
        super(task, metadata);
        this.listId = listId;
        this.taskSeriesId = taskSeriesId;
        this.taskId = taskId;
    }
}