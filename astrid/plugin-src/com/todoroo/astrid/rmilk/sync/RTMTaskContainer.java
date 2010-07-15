package com.todoroo.astrid.rmilk.sync;

import java.util.ArrayList;

import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

public class RTMTaskContainer extends TaskContainer {
    public long listId, taskSeriesId, taskId;

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata,
            long listId, long taskSeriesId, long taskId) {
        this.task = task;
        this.metadata = metadata;
        this.listId = listId;
        this.taskSeriesId = taskSeriesId;
        this.taskId = taskId;
    }
}