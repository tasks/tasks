/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.todoroo.astrid.files.FileUtilities.getNewAttachmentPath;
import static org.tasks.files.FileHelper.copyFile;

public class ActFmCameraModule {

    private static final Logger log = LoggerFactory.getLogger(ActFmCameraModule.class);

    protected static final int REQUEST_CODE_CAMERA = 1;
    protected static final int REQUEST_CODE_PICTURE = 2;

    private static File lastTempFile = null;

    public interface ClearImageCallback {
        public void clearImage();
    }

    public static void showPictureLauncher(final Fragment fragment, final Preferences preferences, final ClearImageCallback clearImageOption) {
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
                    lastTempFile = getFilename(fragment.getActivity(), preferences, ".jpeg");
                    if (lastTempFile == null) {
                        Toast.makeText(fragment.getActivity(), R.string.external_storage_unavailable, Toast.LENGTH_LONG).show();
                        d.dismiss();
                    } else {
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(lastTempFile));
                        fragment.startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                    }
                } else if ((which == 1 && cameraAvailable) || (which == 0 && !cameraAvailable)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    fragment.startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_CODE_PICTURE);
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

    private static File getFilename(Activity activity, Preferences preferences, String extension) {
        AtomicReference<String> nameRef = new AtomicReference<>();
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        try {
            String path = getNewAttachmentPath(preferences, activity, extension, nameRef);
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
        public void handleCameraResult(Uri uri);
    }

    public static boolean activityResult(Activity activity, Preferences preferences, int requestCode, int resultCode, Intent data,
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
            ContentResolver contentResolver = activity.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
            File tempFile = getFilename(activity, preferences, extension);
            log.debug("Writing {} to {}", uri, tempFile);
            try {
                InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                copyFile(inputStream, tempFile.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            activity.setResult(Activity.RESULT_OK);
            cameraResult.handleCameraResult(Uri.fromFile(tempFile));
            return true;
        }
        return false;
    }
}
