package org.tasks.data;

import androidx.room.Embedded;
import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;

public class CaldavTaskContainer {
  @Embedded public Task task;
  @Embedded public CaldavTask caldavTask;

  public Task getTask() {
    return task;
  }

  public CaldavTask getCaldavTask() {
    return caldavTask;
  }

  public String getRemoteId() {
    return caldavTask.getRemoteId();
  }

  public boolean isDeleted() {
    return task.isDeleted();
  }

  public boolean isNew() {
    return Strings.isNullOrEmpty(caldavTask.getVtodo());
  }
}
