package org.tasks.receivers;

import com.google.common.base.Strings;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.sync.SyncAdapters;

public class GoogleTaskPusher {

  private final SyncAdapters syncAdapters;

  @Inject
  public GoogleTaskPusher(SyncAdapters syncAdapters) {
    this.syncAdapters = syncAdapters;
  }

  void push(Task task, Task original) {
    if (!syncAdapters.isGoogleTaskSyncEnabled()) {
      return;
    }

    if (task.checkTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC)) {
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
      syncAdapters.requestSynchronization();
    }
  }
}
