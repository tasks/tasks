package org.tasks.receivers;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.jobs.JobManager;
import org.tasks.sync.SyncAdapters;
import timber.log.Timber;

public class PushReceiver {

  private final SyncAdapters syncAdapters;
  private final JobManager jobManager;

  @Inject
  public PushReceiver(SyncAdapters syncAdapters, JobManager jobManager) {
    this.syncAdapters = syncAdapters;
    this.jobManager = jobManager;
  }

  public void push(Task task, Task original) {
    boolean googleTaskSyncEnabled = syncAdapters.isGoogleTaskSyncEnabled();
    boolean caldavSyncEnabled = syncAdapters.isCaldavSyncEnabled();
    if (!(googleTaskSyncEnabled || caldavSyncEnabled)) {
      return;
    }
    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
      Timber.d("Suppressed sync: %s", task);
      return;
    }
    if (task.checkAndClearTransitory(SyncFlags.FORCE_SYNC)
        || (googleTaskSyncEnabled && !task.googleTaskUpToDate(original))
        || (caldavSyncEnabled && !task.caldavUpToDate(original))) {
      jobManager.syncNow();
    }
  }
}
