package org.tasks.activities;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class CreateListViewModel extends CompletableViewModel<TaskList> {
  void createList(GtasksInvoker invoker, String account, String name) {
    run(() -> invoker.forAccount(account).createGtaskList(name));
  }
}
