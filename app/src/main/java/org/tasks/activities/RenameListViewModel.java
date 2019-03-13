package org.tasks.activities;

import android.content.Context;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class RenameListViewModel extends CompletableViewModel<TaskList> {
  void renameList(Context context, GoogleTaskList list, String name) {
    run(
        () ->
            new GtasksInvoker(context, list.getAccount())
                .renameGtaskList(list.getRemoteId(), name));
  }
}
