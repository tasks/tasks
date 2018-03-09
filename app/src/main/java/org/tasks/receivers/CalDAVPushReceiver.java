package org.tasks.receivers;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;

import org.tasks.caldav.CaldavAccountManager;

import javax.inject.Inject;

public class CalDAVPushReceiver {

    private final CaldavAccountManager caldavAccountManager;

    @Inject
    public CalDAVPushReceiver(CaldavAccountManager caldavAccountManager) {
        this.caldavAccountManager = caldavAccountManager;
    }

    public void push(Task task, Task original) {
        if(task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
            return;
        }
        caldavAccountManager.requestSynchronization();
    }
}
