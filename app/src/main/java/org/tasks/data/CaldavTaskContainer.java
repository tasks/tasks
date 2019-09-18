package org.tasks.data;

import androidx.room.Embedded;
import com.todoroo.astrid.data.Task;

public class CaldavTaskContainer {
  @Embedded public Task task;
  @Embedded public CaldavTask caldavTask;
}
