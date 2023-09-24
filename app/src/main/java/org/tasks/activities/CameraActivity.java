package org.tasks.activities;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.todoroo.astrid.utility.Constants;

import org.tasks.files.FileHelper;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CameraActivity extends AppCompatActivity {

  private static final int REQUEST_CODE_CAMERA = 75;
  private static final String EXTRA_URI = "extra_output";
  @Inject Preferences preferences;

  private Uri uri;

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      uri = savedInstanceState.getParcelable(EXTRA_URI);
    } else {
      try {
        uri =
            FileHelper.newFile(
                this,
                preferences.getCacheDirectory(),
                "image/jpeg",
                new DateTime().toString("yyyyMMddHHmm"),
                ".jpeg");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (!uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
        throw new RuntimeException("Invalid Uri");
      }
      final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      final Uri shared =
          FileProvider.getUriForFile(
              this, Constants.FILE_PROVIDER_AUTHORITY, new File(uri.getPath()));
      intent.putExtra(MediaStore.EXTRA_OUTPUT, shared);
      intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      if (resultCode == RESULT_OK) {
        final Intent intent = new Intent();
        intent.setData(uri);
        setResult(RESULT_OK, intent);
      }
      finish();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_URI, uri);
  }
}
