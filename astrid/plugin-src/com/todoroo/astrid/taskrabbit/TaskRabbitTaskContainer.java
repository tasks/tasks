package com.todoroo.astrid.taskrabbit;

import java.util.ArrayList;

import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskRabbitTaskContainer extends SyncContainer {

    public static final int NO_ID = 0;

    public Metadata trTask;

    public TaskRabbitTaskContainer(Task task, Metadata trTask) {
        this.task = task;
        this.trTask = trTask;
        if(this.trTask == null) {
            this.trTask = TaskRabbitMetadata.createEmptyMetadata(task.getId());
        }
        this.metadata = new ArrayList<Metadata>();
    }

    public TaskRabbitTaskContainer(Task task) {
        this(task, null);
    }

    public JSONObject getLocalTaskData() {
        return getJSONData(TaskRabbitMetadata.DATA_LOCAL);
    }
    public JSONObject getRemoteTaskData() {
        return getJSONData(TaskRabbitMetadata.DATA_REMOTE);
    }

    public long getTaskID() {
        if(TextUtils.isEmpty(trTask.getValue(TaskRabbitMetadata.ID)))
            return NO_ID;
        try {
            return Long.parseLong(trTask.getValue(TaskRabbitMetadata.ID));
        } catch (Exception e) {
            return NO_ID;
        }
    }

    private JSONObject getJSONData(StringProperty key) {
        if(trTask.containsNonNullValue(key)) {
            String jsonString = trTask.getValue(key);
            if (!TextUtils.isEmpty(jsonString)) {
                try {
                    return new JSONObject(jsonString);
                } catch (Exception e) {
                    Log.e("Task Rabbit task container", //$NON-NLS-1$
                            e.toString());
                }
            }
        }
        return null;
    }
    public void setTaskID(String taskID) {
        trTask.setValue(TaskRabbitMetadata.ID, taskID);
    }
    public void setLocalTaskData(String taskData) {
        trTask.setValue(TaskRabbitMetadata.DATA_LOCAL, taskData);
    }
    public void setRemoteTaskData(String taskData) {
        trTask.setValue(TaskRabbitMetadata.DATA_REMOTE, taskData);
    }

    public boolean isTaskRabbit() {
        return getTaskID() > 0;
    }
}
