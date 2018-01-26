package org.tasks.receivers;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;

import org.tasks.gtasks.SyncAdapterHelper;

import java.util.ArrayList;

import javax.inject.Inject;

public class GoogleTaskPusher {

    private static final Property<?>[] GOOGLE_TASK_PROPERTIES = { Task.ID, Task.TITLE,
            Task.NOTES, Task.DUE_DATE, Task.COMPLETION_DATE, Task.DELETION_DATE };

    private final SyncAdapterHelper syncAdapterHelper;

    @Inject
    public GoogleTaskPusher(SyncAdapterHelper syncAdapterHelper) {
        this.syncAdapterHelper = syncAdapterHelper;
    }

    void push(Task task, ArrayList<String> modifiedValues) {
        if(!syncAdapterHelper.isEnabled()) {
            return;
        }

        if(task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
            return;
        }

        if (checkValuesForProperties(modifiedValues, GOOGLE_TASK_PROPERTIES) || task.checkTransitory(SyncFlags.FORCE_SYNC)) {
            syncAdapterHelper.requestSynchronization();
        }
    }

    private boolean checkValuesForProperties(ArrayList<String> values, Property<?>[] properties) {
        if (values == null) {
            return false;
        }
        for (Property<?> property : properties) {
            if (property != Task.ID && values.contains(property.name)) {
                return true;
            }
        }
        return false;
    }
}
