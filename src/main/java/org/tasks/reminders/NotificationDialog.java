package org.tasks.reminders;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingDialogFragment;

import java.util.List;

import javax.inject.Inject;

import static java.util.Arrays.asList;

public class NotificationDialog extends InjectingDialogFragment {

    public interface NotificationHandler {
        void edit();

        void snooze();

        void complete();

        void dismiss();
    }

    @Inject DialogBuilder dialogBuilder;

    private String title;
    private NotificationHandler handler;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> items = asList(
                getString(R.string.TAd_actionEditTask),
                getString(R.string.rmd_NoA_snooze),
                getString(R.string.rmd_NoA_done));

        handler = (NotificationHandler) getActivity();

        return dialogBuilder.newDialog()
                .setTitle(title)
                .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                handler.edit();
                                break;
                            case 1:
                                handler.snooze();
                                break;
                            case 2:
                                handler.complete();
                                break;
                        }
                    }
                })
                .show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        handler.dismiss();
    }

    public void setTitle(String title) {
        this.title = title;
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle(title);
        }
    }
}
