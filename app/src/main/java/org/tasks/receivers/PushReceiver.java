package org.tasks.receivers;

import com.google.common.base.Strings;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.jobs.JobManager;
import org.tasks.sync.SyncAdapters;

public class PushReceiver {

  private final SyncAdapters syncAdapters;
  private final JobManager jobManager;

  @Inject
  public PushReceiver(SyncAdapters syncAdapters, JobManager jobManager) {
    this.syncAdapters = syncAdapters;
    this.jobManager = jobManager;
  }

  public void push(Task task, Task original) {
    if (!pushGoogleTasks(task, original)) {
      pushCaldav(task, original);
    }
  }

  private boolean pushGoogleTasks(Task task, Task original) {
    if (!syncAdapters.isGoogleTaskSyncEnabled()) {
      return false;
    }

    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
      return false;
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
      syncAdapters.syncNow();
      return true;
    }

    return false;
  }

  private void pushCaldav(Task task, Task original) {
    if (!syncAdapters.isCaldavSyncEnabled()) {
      return;
    }

    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
      return;
    }

    jobManager.syncNow();
  }
}
