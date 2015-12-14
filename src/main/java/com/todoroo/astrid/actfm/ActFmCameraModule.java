/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.DeviceInfo;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

public class ActFmCameraModule {

    private static final Logger log = LoggerFactory.getLogger(ActFmCameraModule.class);

    protected static final int REQUEST_CODE_CAMERA = 1;

    private static File lastTempFile = null;

    private final Fragment fragment;
    private final Preferences preferences;
    private DeviceInfo deviceInfo;
    private DialogBuilder dialogBuilder;

    public interface ClearImageCallback {
        void clearImage();
    }

    @Inject
    public ActFmCameraModule(Fragment fragment, Preferences preferences, DeviceInfo deviceInfo, DialogBuilder dialogBuilder) {
        this.fragment = fragment;
        this.preferences = preferences;
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
                    lastTempFile = getFilename(".jpeg");
                    if (lastTempFile == null) {
                        Toast.makeText(fragment.getActivity(), R.string.external_storage_unavailable, Toast.LENGTH_LONG).show();
                    } else {
                        final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(lastTempFile));
                        fragment.startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                    }
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

    private File getFilename(String extension) {
        AtomicReference<String> nameRef = new AtomicReference<>();
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        try {
            String path = preferences.getNewAttachmentPath(extension, nameRef);
            File file = new File(path);
            file.getParentFile().mkdirs();
            if (!file.createNewFile()) {
                throw new RuntimeException("Failed to create " + file.getPath());
            }
            return file;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public interface CameraResultCallback {
        void handleCameraResult(Uri uri);
    }

    public boolean activityResult(int requestCode, int resultCode, Intent data,
            CameraResultCallback cameraResult) {
        if(requestCode == ActFmCameraModule.REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            if (lastTempFile != null) {
                Uri uri = Uri.fromFile(lastTempFile);
                lastTempFile = null;
                fragment.getActivity().setResult(Activity.RESULT_OK);
                cameraResult.handleCameraResult(uri);
            }
            return true;
        }
        return false;
    }
}
