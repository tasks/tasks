package com.todoroo.astrid.rmilk.sync;

import java.util.ArrayList;

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
    public RtmTaskSeries remote = null;

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata,
            long listId, long taskSeriesId, long taskId) {
        this.task = task;
        this.metadata = metadata;
        this.listId = listId;
        this.taskSeriesId = taskSeriesId;
        this.taskId = taskId;
    }

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata,
            RtmTaskSeries rtmTaskSeries) {
        this(task, metadata, Long.parseLong(rtmTaskSeries.getList().getId()),
                Long.parseLong(rtmTaskSeries.getId()), Long.parseLong(rtmTaskSeries.getTask().getId()));
        remote = rtmTaskSeries;
    }

    public RTMTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this(task, metadata, 0, 0, 0);
        for(Metadata metadatum : metadata) {
            if(MilkTask.METADATA_KEY.equals(metadatum.getValue(Metadata.KEY))) {
                listId = metadatum.getValue(MilkTask.LIST_ID);
                taskSeriesId = metadatum.getValue(MilkTask.TASK_SERIES_ID);
                taskId = metadatum.getValue(MilkTask.TASK_ID);
                break;
            }
        }
    }


}