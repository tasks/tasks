package org.tasks.activities;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.files.FileExplore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class AddAttachmentActivity extends InjectingAppCompatActivity implements DialogInterface.OnCancelListener, AddAttachmentDialog.AddAttachmentCallback {

    private static final Logger log = LoggerFactory.getLogger(AddAttachmentActivity.class);
    private static final String FRAG_TAG_ATTACHMENT_DIALOG = "frag_tag_attachment_dialog";
    private static final int REQUEST_CAMERA = 12120;
    private static final int REQUEST_GALLERY = 12121;
    private static final int REQUEST_STORAGE = 12122;

    public static final String EXTRA_PATH = "extra_path";

    @Deprecated private static File lastTempFile = null;

    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        AddAttachmentDialog dialog = (AddAttachmentDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_ATTACHMENT_DIALOG);
        if (dialog == null) {
            dialog = new AddAttachmentDialog();
            dialog.show(supportFragmentManager, FRAG_TAG_ATTACHMENT_DIALOG);
        }
        dialog.setOnCancelListener(this);
        dialog.setAddAttachmentCallback(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void takePicture() {
        lastTempFile = getFilename(".jpeg");
        if (lastTempFile == null) {
            Toast.makeText(this, R.string.external_storage_unavailable, Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri = Uri.fromFile(lastTempFile);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            if (atLeastLollipop()) {
                intent.setClipData(ClipData.newRawUri(null, uri));
            }
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {{
            setType("image/*");
        }};
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
                if (lastTempFile != null) {
                    log.info("Saved {}", lastTempFile.getAbsolutePath());
                    setResult(RESULT_OK, new Intent() {{
                        putExtra(EXTRA_PATH, lastTempFile.getAbsolutePath());
                    }});
                    lastTempFile = null;
                }
            }
            finish();
        } else if (requestCode == REQUEST_GALLERY) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                ContentResolver contentResolver = getContentResolver();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
                final File tempFile = getFilename(extension);
                log.info("Writing {} to {}", uri, tempFile);
                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    copyFile(inputStream, tempFile.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                setResult(RESULT_OK, new Intent() {{
                    putExtra(EXTRA_PATH, tempFile.getAbsolutePath());
                }});
            }
            finish();
        } else if (requestCode == REQUEST_STORAGE) {
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FileExplore.RESULT_FILE_SELECTED);
                final String destination = copyToAttachmentDirectory(path);
                if (destination != null) {
                    log.info("Copied {} to {}", path, destination);
                    setResult(RESULT_OK, new Intent() {{
                        putExtra(EXTRA_PATH, destination);
                    }});
                }
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
            log.error(e.getMessage(), e);
            Toast.makeText(this, R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return null;
        }

        return dst.getAbsolutePath();
    }
}
