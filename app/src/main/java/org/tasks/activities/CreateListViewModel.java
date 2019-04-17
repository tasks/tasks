package org.tasks.activities;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class CreateListViewModel extends CompletableViewModel<TaskList> {
  void createList(GoogleAccountManager googleAccountManager, String account, String name) {
    run(() -> new GtasksInvoker(account, googleAccountManager).createGtaskList(name));
  }
}
