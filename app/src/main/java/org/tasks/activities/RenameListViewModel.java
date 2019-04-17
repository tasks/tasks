package org.tasks.activities;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class RenameListViewModel extends CompletableViewModel<TaskList> {
  void renameList(GoogleAccountManager googleAccountManager, GoogleTaskList list, String name) {
    run(
        () ->
            new GtasksInvoker(list.getAccount(), googleAccountManager)
                .renameGtaskList(list.getRemoteId(), name));
  }
}
