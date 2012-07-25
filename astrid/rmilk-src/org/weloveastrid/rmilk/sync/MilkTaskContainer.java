/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.weloveastrid.rmilk.api.data.RtmTaskSeries;
import org.weloveastrid.rmilk.data.MilkTaskFields;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkTaskContainer extends SyncContainer {
    public long listId, taskSeriesId, taskId;
    public boolean repeating;
    public RtmTaskSeries remote;

    public MilkTaskContainer(Task task, ArrayList<Metadata> metadata,
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

    public MilkTaskContainer(Task task, ArrayList<Metadata> metadata,
            RtmTaskSeries rtmTaskSeries) {
        this(task, metadata, Long.parseLong(rtmTaskSeries.getList().getId()),
                Long.parseLong(rtmTaskSeries.getId()), Long.parseLong(rtmTaskSeries.getTask().getId()),
                rtmTaskSeries.hasRecurrence(), rtmTaskSeries);
    }

    public MilkTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this(task, metadata, 0, 0, 0, false, null);
        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(MilkTaskFields.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                if(item.containsNonNullValue(MilkTaskFields.LIST_ID))
                    listId = item.getValue(MilkTaskFields.LIST_ID);
                if(item.containsNonNullValue(MilkTaskFields.TASK_SERIES_ID))
                    taskSeriesId = item.getValue(MilkTaskFields.TASK_SERIES_ID);
                if(item.containsNonNullValue(MilkTaskFields.TASK_ID))
                    taskId = item.getValue(MilkTaskFields.TASK_ID);
                if(item.containsNonNullValue(MilkTaskFields.REPEATING))
                    repeating = item.getValue(MilkTaskFields.REPEATING) == 1;
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void prepareForSaving() {
        super.prepareForSaving();
        metadata.add(MilkTaskFields.create(this));
    }

}
