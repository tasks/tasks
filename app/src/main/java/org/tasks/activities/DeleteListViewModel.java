package org.tasks.activities;

import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.ui.ActionViewModel;

@SuppressWarnings("WeakerAccess")
public class DeleteListViewModel extends ActionViewModel {
  void deleteList(GtasksInvoker invoker, GoogleTaskList list) {
    run(() -> invoker.forAccount(list.getAccount()).deleteGtaskList(list.getRemoteId()));
  }
}
