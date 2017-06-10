package org.tasks.activities;

import android.support.v4.app.FragmentManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.AACRecordingActivity;

import org.tasks.R;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.files.FileExplore;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AddAttachmentActivity extends InjectingAppCompatActivity implements DialogInterface.OnCancelListener, AddAttachmentDialog.AddAttachmentCallback {

    private static final String FRAG_TAG_ATTACHMENT_DIALOG = "frag_tag_attachment_dialog";

    private static final int REQUEST_CAMERA = 12120;
    private static final int REQUEST_GALLERY = 12121;
    private static final int REQUEST_STORAGE = 12122;
    private static final int REQUEST_CODE_RECORD = 12123;

    public static final String EXTRA_PATH = "extra_path";
    public static final String EXTRA_TYPE = "extra_type";

    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getSupportFragmentManager();
        AddAttachmentDialog dialog = (AddAttachmentDialog) fragmentManager.findFragmentByTag(FRAG_TAG_ATTACHMENT_DIALOG);
        if (dialog == null) {
            dialog = new AddAttachmentDialog();
            dialog.show(fragmentManager, FRAG_TAG_ATTACHMENT_DIALOG);
        }
        dialog.setOnCancelListener(this);
        dialog.setAddAttachmentCallback(this);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void takePicture() {
        startActivityForResult(new Intent(this, CameraActivity.class), REQUEST_CAMERA);
    }

    @Override
    public void recordNote() {
        startActivityForResult(new Intent(this, AACRecordingActivity.class), REQUEST_CODE_RECORD);
    }

    @Override
    public void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_GALLERY);
        }
    }

    @Override
    public void pickFromStorage() {
        startActivityForResult(new Intent(this, FileExplore.class), REQUEST_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getParcelableExtra(CameraActivity.EXTRA_URI);
                final File file = new File(uri.getPath());
                String path = file.getPath();
                Timber.i("Saved %s", file.getAbsolutePath());
                final String extension = path.substring(path.lastIndexOf('.') + 1);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_PATH, file.getAbsolutePath());
                intent.putExtra(EXTRA_TYPE, TaskAttachment.FILE_TYPE_IMAGE + extension);
                setResult(RESULT_OK, intent);
            }
            finish();
        } else if (requestCode == REQUEST_CODE_RECORD) {
            if (resultCode == RESULT_OK) {
                final String recordedAudioPath = data.getStringExtra(AACRecordingActivity.RESULT_OUTFILE);
                final String extension = recordedAudioPath.substring(recordedAudioPath.lastIndexOf('.') + 1);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_PATH, recordedAudioPath);
                intent.putExtra(EXTRA_TYPE, TaskAttachment.FILE_TYPE_AUDIO + extension);
                setResult(RESULT_OK, intent);
            }
            finish();
        } else if (requestCode == REQUEST_GALLERY) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                ContentResolver contentResolver = getContentResolver();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                final String extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
                final File tempFile = getFilename(extension);
                Timber.i("Writing %s to %s", uri, tempFile);
                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    copyFile(inputStream, tempFile.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Intent intent = new Intent();
                intent.putExtra(EXTRA_PATH, tempFile.getAbsolutePath());
                intent.putExtra(EXTRA_TYPE, TaskAttachment.FILE_TYPE_IMAGE + extension);
                setResult(RESULT_OK, intent);
            }
            finish();
        } else if (requestCode == REQUEST_STORAGE) {
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FileExplore.EXTRA_FILE);
                final String destination = copyToAttachmentDirectory(path);
                if (destination != null) {
                    Timber.i("Copied %s to %s", path, destination);
                    final String extension = destination.substring(path.lastIndexOf('.') + 1);
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_PATH, destination);
                    intent.putExtra(EXTRA_TYPE, TaskAttachment.FILE_TYPE_IMAGE + extension);
                    setResult(RESULT_OK, intent);
                }
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private File getFilename(String extension) {
        AtomicReference<String> nameRef = new AtomicReference<>();
        if (isNullOrEmpty(extension)) {
            extension = "";
        } else if (!extension.startsWith(".")) {
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

    private void copyFile(InputStream inputStream, String to) throws IOException {
        FileOutputStream fos = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) != -1) {
            fos.write(buf, 0, len);
        }
        fos.close();
    }

    private String copyToAttachmentDirectory(String file) {
        File src = new File(file);
        if (!src.exists()) {
            Toast.makeText(this, R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return null;
        }

        File dst = new File(preferences.getAttachmentsDirectory() + File.separator + src.getName());
        try {
            AndroidUtilities.copyFile(src, dst);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(this, R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return null;
        }

        return dst.getAbsolutePath();
    }
}
