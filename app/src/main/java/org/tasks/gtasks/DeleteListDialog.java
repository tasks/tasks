package org.tasks.gtasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

public class DeleteListDialog extends InjectingDialogFragment {

  private static final String EXTRA_LIST = "extra_list";
  @Inject @ForApplication Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject PlayServices playServices;
  private DeleteListDialogCallback callback;
  private GoogleTaskList googleTaskList;
  private ProgressDialog dialog;

  public static DeleteListDialog newDeleteListDialog(GoogleTaskList googleTaskList) {
    DeleteListDialog dialog = new DeleteListDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_LIST, googleTaskList);
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    Bundle arguments = getArguments();
    googleTaskList = arguments.getParcelable(EXTRA_LIST);
    dialog = dialogBuilder.newProgressDialog(R.string.deleting_list);
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

    callback = (DeleteListDialogCallback) activity;
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  private void execute() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... voids) {
        try {
          new GtasksInvoker(context, playServices, googleTaskList.getAccount())
              .deleteGtaskList(googleTaskList.getRemoteId());
          return true;
        } catch (IOException e) {
          Timber.e(e);
          return false;
        }
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (dialog.isShowing()) {
          dialog.dismiss();
        }

        if (result) {
          callback.onListDeleted();
        } else {
          callback.requestFailed();
        }
      }
    }.execute();
  }

  public interface DeleteListDialogCallback {

    void onListDeleted();

    void requestFailed();
  }
}
