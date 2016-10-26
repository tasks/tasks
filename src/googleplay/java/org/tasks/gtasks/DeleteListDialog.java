package org.tasks.gtasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.todoroo.astrid.gtasks.api.GtasksInvoker;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

public class DeleteListDialog extends InjectingDialogFragment {

    public static DeleteListDialog newDeleteListDialog(String id) {
        DeleteListDialog dialog = new DeleteListDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_ID, id);
        dialog.setArguments(args);
        return dialog;
    }

    public interface DeleteListDialogCallback {
        void onListDeleted();

        void requestFailed();
    }

    private static final String EXTRA_ID = "extra_id";

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksInvoker gtasksInvoker;

    private DeleteListDialogCallback callback;
    private String id;
    private ProgressDialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle arguments = getArguments();
        id = arguments.getString(EXTRA_ID);
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
                    gtasksInvoker.deleteGtaskList(id);
                    return true;
                } catch (IOException e) {
                    Timber.e(e, e.getMessage());
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
}
