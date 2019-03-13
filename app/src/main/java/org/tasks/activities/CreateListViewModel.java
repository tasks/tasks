package org.tasks.activities;

import android.content.Context;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class CreateListViewModel extends CompletableViewModel<TaskList> {
  void createList(Context context, String account, String name) {
    run(() -> new GtasksInvoker(context, account).createGtaskList(name));
  }
}
