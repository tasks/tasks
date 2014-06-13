/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;

import com.todoroo.andlib.utility.DateUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.tasks.files.FileHelper.getPathFromUri;

public class ActFmCameraModule {

    private static final Logger log = LoggerFactory.getLogger(ActFmCameraModule.class);

    protected static final int REQUEST_CODE_CAMERA = 1;
    protected static final int REQUEST_CODE_PICTURE = 2;

    private static File lastTempFile = null;

    public interface ClearImageCallback {
        public void clearImage();
    }

    public static void showPictureLauncher(final Fragment fragment, final ClearImageCallback clearImageOption) {
        ArrayList<String> options = new ArrayList<>();

        final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager pm = fragment.getActivity().getPackageManager();
        final boolean cameraAvailable = pm.queryIntentActivities(cameraIntent, 0).size() > 0;
        if(cameraAvailable) {
            options.add(fragment.getString(R.string.actfm_picture_camera));
        }

        options.add(fragment.getString(R.string.actfm_picture_gallery));

        if (clearImageOption != null) {
            options.add(fragment.getString(R.string.actfm_picture_clear));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(fragment.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if(which == 0 && cameraAvailable) {
                    lastTempFile = getTempFile(fragment.getActivity());
                    if (lastTempFile != null) {
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(lastTempFile));
                    }
                    fragment.startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                } else if ((which == 1 && cameraAvailable) || (which == 0 && !cameraAvailable)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    fragment.startActivityForResult(Intent.createChooser(intent,
                            fragment.getString(R.string.actfm_TVA_tag_picture)), REQUEST_CODE_PICTURE);
                } else {
                    if (clearImageOption != null) {
                        clearImageOption.clearImage();
                    }
                }
            }
        };

        // show a menu of available options
        new AlertDialog.Builder(fragment.getActivity())
        .setAdapter(adapter, listener)
        .show().setOwnerActivity(fragment.getActivity());
    }

    private static File getTempFile(Activity activity) {
        try {
            String storageState = Environment.getExternalStorageState();
            if(storageState.equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory().getPath() + File.separatorChar + "Android/data/" + activity.getPackageName() + "/files/";
                return File.createTempFile("comment_pic_" + DateUtilities.now(), ".jpg", new File(path));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public interface CameraResultCallback {
        public void handleCameraResult(Uri uri);
    }

    public static boolean activityResult(Activity activity, int requestCode, int resultCode, Intent data,
            CameraResultCallback cameraResult) {
        if(requestCode == ActFmCameraModule.REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            if (lastTempFile != null) {
                Uri uri = Uri.fromFile(lastTempFile);
                lastTempFile = null;
                activity.setResult(Activity.RESULT_OK);
                cameraResult.handleCameraResult(uri);
            }
            return true;
        } else if(requestCode == ActFmCameraModule.REQUEST_CODE_PICTURE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = getPathFromUri(activity, uri);
            if (new File(path).exists()) {
                activity.setResult(Activity.RESULT_OK);
                cameraResult.handleCameraResult(uri);
            }
            return true;
        }
        return false;
    }
}
