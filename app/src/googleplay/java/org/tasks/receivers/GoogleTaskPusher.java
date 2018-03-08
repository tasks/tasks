package org.tasks.receivers;

import com.google.common.base.Strings;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;

import org.tasks.gtasks.GtaskSyncAdapterHelper;

import javax.inject.Inject;

public class GoogleTaskPusher {

    private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;

    @Inject
    public GoogleTaskPusher(GtaskSyncAdapterHelper gtaskSyncAdapterHelper) {
        this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    }

    void push(Task task, Task original) {
        if(!gtaskSyncAdapterHelper.isEnabled()) {
            return;
        }

        if(task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
            return;
        }

        if (original == null ||
                !task.getTitle().equals(original.getTitle()) ||
                (Strings.isNullOrEmpty(task.getNotes())
                        ? !Strings.isNullOrEmpty(original.getNotes())
                        : !task.getNotes().equals(original.getNotes())) ||
                !task.getDueDate().equals(original.getDueDate()) ||
                !task.getCompletionDate().equals(original.getCompletionDate()) ||
                !task.getDeletionDate().equals(original.getDeletionDate()) ||
                task.checkAndClearTransitory(SyncFlags.FORCE_SYNC)) {
            gtaskSyncAdapterHelper.requestSynchronization();
        }
    }
}
