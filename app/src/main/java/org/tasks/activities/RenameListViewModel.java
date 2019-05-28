package org.tasks.activities;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class RenameListViewModel extends CompletableViewModel<TaskList> {
  void renameList(GtasksInvoker invoker, GoogleTaskList list, String name) {
    run(() -> invoker.forAccount(list.getAccount()).renameGtaskList(list.getRemoteId(), name));
  }
}
