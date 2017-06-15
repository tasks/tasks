package org.tasks.gtasks;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateListDialog extends InjectingDialogFragment {

    public static CreateListDialog newCreateListDialog(String name) {
        CreateListDialog dialog = new CreateListDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_NAME, name);
        dialog.setArguments(args);
        return dialog;
    }

    public interface CreateListDialogCallback {
        void onListCreated(TaskList taskList);

        void requestFailed();
    }

    private static final String EXTRA_NAME = "extra_name";

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksInvoker gtasksInvoker;

    private CreateListDialogCallback callback;
    private ProgressDialog dialog;
    private String name;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle arguments = getArguments();
        name = arguments.getString(EXTRA_NAME);
        dialog = dialogBuilder.newProgressDialog(R.string.creating_new_list);
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

        callback = (CreateListDialogCallback) activity;
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
                    return gtasksInvoker.createGtaskList(name);
                } catch (IOException e) {
                    Timber.e(e, e.getMessage());
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
                    callback.onListCreated(taskList);
                }
            }
        }.execute();
    }
}
