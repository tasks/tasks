package org.tasks.receivers;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.caldav.CaldavAccountManager;
import org.tasks.sync.SyncAdapters;

public class CalDAVPushReceiver {

  private final CaldavAccountManager caldavAccountManager;
  private final SyncAdapters syncAdapters;

  @Inject
  public CalDAVPushReceiver(CaldavAccountManager caldavAccountManager, SyncAdapters syncAdapters) {
    this.caldavAccountManager = caldavAccountManager;
    this.syncAdapters = syncAdapters;
  }

  public void push(Task task, Task original) {
    if (!syncAdapters.isCaldavSyncEnabled()) {
      return;
    }

    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
      return;
    }

    caldavAccountManager.requestSynchronization();
  }
}
