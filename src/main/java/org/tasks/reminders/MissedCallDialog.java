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

public class MissedCallDialog extends InjectingDialogFragment {

    public interface MissedCallHandler {
        void callNow();

        void callLater();

        void ignore();
    }

    @Inject DialogBuilder dialogBuilder;

    private DialogInterface.OnDismissListener onDismissListener;
    private String title;

    MissedCallHandler handler;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> actions = asList(
                getString(R.string.MCA_return_call),
                getString(R.string.MCA_add_task),
                getString(R.string.MCA_ignore));

        handler = (MissedCallHandler) getActivity();

        return dialogBuilder.newDialog()
                .setTitle(title)
                .setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                handler.callNow();
                                break;
                            case 1:
                                handler.callLater();
                                break;
                            default:
                                handler.ignore();
                        }
                    }
                })
                .show();
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
