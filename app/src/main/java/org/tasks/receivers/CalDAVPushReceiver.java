package org.tasks.receivers;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.jobs.JobManager;
import org.tasks.sync.SyncAdapters;

public class CalDAVPushReceiver {

  private final JobManager jobManager;
  private final SyncAdapters syncAdapters;

  @Inject
  public CalDAVPushReceiver(JobManager jobManager, SyncAdapters syncAdapters) {
    this.jobManager = jobManager;
    this.syncAdapters = syncAdapters;
  }

  public void push(Task task, Task original) {
    if (!syncAdapters.isCaldavSyncEnabled()) {
      return;
    }

    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
      return;
    }

    jobManager.syncCaldavNow();
  }
}
