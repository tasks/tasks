package org.tasks.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;

public abstract class ProgressDialogAsyncTask extends AsyncTask<Void, Void, Integer> {

    private static final Logger log = LoggerFactory.getLogger(ProgressDialogAsyncTask.class);

    ProgressDialog progressDialog;
    private Activity activity;
    private DialogBuilder dialogBuilder;

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
                log.error(e.getMessage(), e);
            }
        }

        Toast.makeText(activity, activity.getString(getResultResource(), integer), Toast.LENGTH_LONG).show();
    }

    protected abstract int getResultResource();
}
