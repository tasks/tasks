package org.tasks.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.DeviceInfo;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;

public class AddAttachmentDialog extends InjectingDialogFragment {

    public interface AddAttachmentCallback {
        void takePicture();

        void pickFromGallery();

        void pickFromStorage();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject DeviceInfo deviceInfo;

    private AddAttachmentCallback callback;
    private DialogInterface.OnCancelListener onCancelListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> entries = newArrayList();
        final List<Runnable> actions = newArrayList();
        if (deviceInfo.hasCamera()) {
            entries.add(getString(R.string.take_a_picture));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    callback.takePicture();
                }
            });
        }
        if (deviceInfo.hasGallery()) {
            entries.add(getString(R.string.pick_from_gallery));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    callback.pickFromGallery();
                }
            });
        }
        entries.add(getString(R.string.pick_from_storage));
        actions.add(new Runnable() {
            @Override
            public void run() {
                callback.pickFromStorage();
            }
        });
        return dialogBuilder.newDialog()
                .setItems(entries.toArray(new String[entries.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actions.get(which).run();
                    }
                })
                .show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (onCancelListener != null) {
            onCancelListener.onCancel(dialog);
        }
    }

    public void setAddAttachmentCallback(AddAttachmentCallback callback) {
        this.callback = callback;
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }
}
