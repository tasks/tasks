/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ArrayAdapter;

import com.todoroo.astrid.activity.TaskEditFragment;

import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ActFmCameraModule {

    private final Fragment fragment;
    private DeviceInfo deviceInfo;
    private DialogBuilder dialogBuilder;

    public interface ClearImageCallback {
        void clearImage();
    }

    @Inject
    public ActFmCameraModule(Fragment fragment, DeviceInfo deviceInfo, DialogBuilder dialogBuilder) {
        this.fragment = fragment;
        this.deviceInfo = deviceInfo;
        this.dialogBuilder = dialogBuilder;
    }

    public void showPictureLauncher(final ClearImageCallback clearImageOption) {
        final List<Runnable> runnables = new ArrayList<>();
        List<String> options = new ArrayList<>();

        final boolean cameraAvailable = deviceInfo.hasCamera();
        if (cameraAvailable) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    fragment.startActivityForResult(new Intent(fragment.getActivity(), CameraActivity.class), TaskEditFragment.REQUEST_CODE_CAMERA);
                }
            });
            options.add(fragment.getString(R.string.take_a_picture));
        }

        if (clearImageOption != null) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    clearImageOption.clearImage();
                }
            });
            options.add(fragment.getString(R.string.actfm_picture_clear));
        }

        if (runnables.size() == 1) {
            runnables.get(0).run();
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(fragment.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    runnables.get(which).run();
                    d.dismiss();
                }
            };

            // show a menu of available options
            dialogBuilder.newDialog()
                    .setAdapter(adapter, listener)
                    .show().setOwnerActivity(fragment.getActivity());
        }
    }
}
