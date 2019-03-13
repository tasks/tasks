package org.tasks.activities;

import android.content.Context;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import org.tasks.data.GoogleTaskList;
import org.tasks.ui.ActionViewModel;

@SuppressWarnings("WeakerAccess")
public class DeleteListViewModel extends ActionViewModel {
  void deleteList(Context context, GoogleTaskList list) {
    run(() -> new GtasksInvoker(context, list.getAccount()).deleteGtaskList(list.getRemoteId()));
  }
}
