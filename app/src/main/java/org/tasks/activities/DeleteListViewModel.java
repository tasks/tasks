package org.tasks.activities;

import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.ui.ActionViewModel;

@SuppressWarnings("WeakerAccess")
public class DeleteListViewModel extends ActionViewModel {
  void deleteList(GoogleAccountManager googleAccountManager, GoogleTaskList list) {
    run(() -> new GtasksInvoker(list.getAccount(), googleAccountManager).deleteGtaskList(list.getRemoteId()));
  }
}
