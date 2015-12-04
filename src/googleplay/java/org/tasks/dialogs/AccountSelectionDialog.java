package org.tasks.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.tasks.AccountManager;
import org.tasks.R;
import org.tasks.injection.InjectingDialogFragment;

import java.util.List;

import javax.inject.Inject;

public class AccountSelectionDialog extends InjectingDialogFragment {

    private AccountSelectionHandler handler;

    public interface AccountSelectionHandler {
        void accountSelected(String account);

        void onCancel();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject AccountManager accountManager;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<String> accounts = accountManager.getAccounts();

        return dialogBuilder.newDialog()
                .setTitle(R.string.choose_google_account)
                .setItems(accounts.toArray(new String[accounts.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.accountSelected(accounts.get(which));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null) {
                            handler.onCancel();
                        }
                    }
                })
                .show();
    }

    public void setAccountSelectionHandler(AccountSelectionHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (handler != null) {
            handler.onCancel();
        }
    }
}
