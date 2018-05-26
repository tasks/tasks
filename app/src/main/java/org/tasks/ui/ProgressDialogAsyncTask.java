package org.tasks.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import timber.log.Timber;

public abstract class ProgressDialogAsyncTask extends AsyncTask<Void, Void, Integer> {

  private final Activity activity;
  private final DialogBuilder dialogBuilder;
  private ProgressDialog progressDialog;

  public ProgressDialogAsyncTask(Activity activity, DialogBuilder dialogBuilder) {
    this.activity = activity;
    this.dialogBuilder = dialogBuilder;
  }

  @Override
  protected void onPreExecute() {
    progressDialog = dialogBuilder.newProgressDialog(R.string.DLG_wait);
    progressDialog.show();
  }

  @Override
  protected void onPostExecute(Integer integer) {
    if (progressDialog.isShowing()) {
      try {
        progressDialog.dismiss();
      } catch (Exception e) {
        Timber.e(e);
      }
    }

    Toast.makeText(activity, activity.getString(getResultResource(), integer), Toast.LENGTH_LONG)
        .show();
  }

  protected abstract int getResultResource();
}
