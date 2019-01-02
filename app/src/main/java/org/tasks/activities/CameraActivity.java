package org.tasks.activities;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.todoroo.astrid.utility.Constants;

import org.tasks.files.FileHelper;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import androidx.core.content.FileProvider;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class CameraActivity extends InjectingAppCompatActivity {

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
      intent.putExtra(
          MediaStore.EXTRA_OUTPUT,
          FileProvider.getUriForFile(
              this, Constants.FILE_PROVIDER_AUTHORITY, new File(uri.getPath())));
      if (atLeastLollipop()) {
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      } else {
        List<ResolveInfo> resolveInfoList =
            getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
          grantUriPermission(
              resolveInfo.activityInfo.packageName,
              uri,
              Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
      }

      startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      if (resultCode == RESULT_OK) {
          Intent intent = new Intent();
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
