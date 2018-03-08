package org.tasks.caldav;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.todoroo.astrid.helper.UUIDHelper;

import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class RenameAccountDialog extends InjectingDialogFragment {

    public static RenameAccountDialog newRenameAccountDialog(String uuid, String name) {
        RenameAccountDialog dialog = new RenameAccountDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_NAME, name);
        args.putString(EXTRA_UUID, uuid);
        dialog.setArguments(args);
        return dialog;
    }

    public interface RenameAccountDialogCallback {
        void onListRenamed();

        void renameFailed();
    }

    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_UUID = "extra_uuid";

    @Inject DialogBuilder dialogBuilder;
    @Inject CaldavAccountManager caldavAccountManager;
    @Inject CaldavDao caldavDao;

    private RenameAccountDialogCallback callback;
    private String name;
    private String uuid;
    private ProgressDialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle arguments = getArguments();
        name = arguments.getString(EXTRA_NAME);
        uuid = arguments.getString(EXTRA_UUID);
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

        callback = (RenameAccountDialogCallback) activity;
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    private void execute() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                CaldavAccount caldavAccount = caldavDao.getAccount(uuid);
                caldavAccount.setName(name);
                Account old = caldavAccountManager.getAccount(uuid);
                if (!caldavAccountManager.addAccount(caldavAccount, old.getPassword())) {
                    return false;
                }
                caldavDao.update(caldavAccount);
                old.setUuid(null);
                caldavAccountManager.removeAccount(old);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                if (result) {
                    callback.onListRenamed();
                } else {
                    callback.renameFailed();
                }
            }
        }.execute();
    }
}
