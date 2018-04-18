package org.tasks.gtasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskList;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import timber.log.Timber;

public class RenameListDialog extends InjectingDialogFragment {

  private static final String EXTRA_NAME = "extra_name";
  private static final String EXTRA_LIST = "extra_list";
  @Inject @ForApplication Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject PlayServices playServices;
  private RenameListDialogCallback callback;
  private ProgressDialog dialog;
  private GoogleTaskList googleTaskList;
  private String name;

  public static RenameListDialog newRenameListDialog(GoogleTaskList googleTaskList, String name) {
    RenameListDialog dialog = new RenameListDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_LIST, googleTaskList);
    args.putString(EXTRA_NAME, name);
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    Bundle arguments = getArguments();
    googleTaskList = arguments.getParcelable(EXTRA_LIST);
    name = arguments.getString(EXTRA_NAME);
    dialog = dialogBuilder.newProgressDialog(R.string.renaming_list);
    execute();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return dialog;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (RenameListDialogCallback) activity;
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  private void execute() {
    new AsyncTask<Void, Void, TaskList>() {
      @Override
      protected TaskList doInBackground(Void... voids) {
        try {
          return new GtasksInvoker(context, playServices, googleTaskList.getAccount())
              .renameGtaskList(googleTaskList.getRemoteId(), name);
        } catch (IOException e) {
          Timber.e(e);
          return null;
        }
      }

      @Override
      protected void onPostExecute(TaskList taskList) {
        if (dialog.isShowing()) {
          dialog.dismiss();
        }

        if (taskList == null) {
          callback.requestFailed();
        } else {
          callback.onListRenamed(taskList);
        }
      }
    }.execute();
  }

  public interface RenameListDialogCallback {

    void onListRenamed(TaskList taskList);

    void requestFailed();
  }
}
