package org.tasks.caldav;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

public class DeleteAccountDialog extends InjectingDialogFragment {

  private static final String EXTRA_UUID = "extra_uuid";
  @Inject DialogBuilder dialogBuilder;
  @Inject CaldavAccountManager caldavAccountManager;
  private DeleteAccountDialogCallback callback;
  private String uuid;
  private ProgressDialog dialog;

  public static DeleteAccountDialog newDeleteAccountDialog(String uuid) {
    DeleteAccountDialog dialog = new DeleteAccountDialog();
    Bundle args = new Bundle();
    args.putString(EXTRA_UUID, uuid);
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    Bundle arguments = getArguments();
    uuid = arguments.getString(EXTRA_UUID);
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

    callback = (DeleteAccountDialogCallback) activity;
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  private void execute() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... voids) {
        Account account = caldavAccountManager.getAccount(uuid);
        return account == null || caldavAccountManager.removeAccount(account);
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (dialog.isShowing()) {
          dialog.dismiss();
        }

        if (result) {
          callback.onListDeleted();
        } else {
          callback.deleteAccountFailed();
        }
      }
    }.execute();
  }

  public interface DeleteAccountDialogCallback {

    void onListDeleted();

    void deleteAccountFailed();
  }
}
