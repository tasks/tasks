package org.tasks.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class CameraActivity extends InjectingAppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 75;
    private static final String EXTRA_OUTPUT = "extra_output";

    public static final String EXTRA_URI = "extra_uri";

    @Inject Preferences preferences;
    @Inject Activity activity;

    private File output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            output = (File) savedInstanceState.getSerializable(EXTRA_OUTPUT);
        } else {
            output = getFilename(".jpeg");
            if (output == null) {
                Toast.makeText(activity, R.string.external_storage_unavailable, Toast.LENGTH_LONG).show();
            } else {
                final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri uri = Uri.fromFile(output);
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                if (atLeastLollipop()) {
                    intent.setClipData(ClipData.newRawUri(null, uri));
                }
                activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (resultCode == RESULT_OK) {
                if (output != null) {
                    final Uri uri = Uri.fromFile(output);
                    setResult(RESULT_OK, new Intent() {{
                        putExtra(EXTRA_URI, uri);
                    }});
                }
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(EXTRA_OUTPUT, output);
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
            Timber.e(e, e.getMessage());
        }
        return null;
    }
}
