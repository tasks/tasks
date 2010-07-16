package com.todoroo.astrid.rmilk.sync;

import java.util.ArrayList;
import java.util.Iterator;

import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.api.data.RtmTaskSeries;
import com.todoroo.astrid.rmilk.data.MilkTask;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RTMTaskContainer extends TaskContainer {
    public long listId, taskSeriesId, taskId;
    public boolean repeating;
    public RtmTaskSeries remote;

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata,
            long listId, long taskSeriesId, long taskId, boolean repeating,
            RtmTaskSeries remote) {
        this.task = task;
        this.metadata = metadata;
        this.listId = listId;
        this.taskSeriesId = taskSeriesId;
        this.taskId = taskId;
        this.repeating = repeating;
        this.remote = remote;
    }

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata,
            RtmTaskSeries rtmTaskSeries) {
        this(task, metadata, Long.parseLong(rtmTaskSeries.getList().getId()),
                Long.parseLong(rtmTaskSeries.getId()), Long.parseLong(rtmTaskSeries.getTask().getId()),
                rtmTaskSeries.hasRecurrence(), rtmTaskSeries);
    }

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this(task, metadata, 0, 0, 0, false, null);
        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(MilkTask.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                listId = item.getValue(MilkTask.LIST_ID);
                taskSeriesId = item.getValue(MilkTask.TASK_SERIES_ID);
                taskId = item.getValue(MilkTask.TASK_ID);
                repeating = item.getValue(MilkTask.REPEATING) == 1;
                iterator.remove();
                break;
            }
        }
    }


}