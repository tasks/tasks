package org.tasks.activities;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
import com.todoroo.astrid.utility.Constants;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class CameraActivity extends InjectingAppCompatActivity {

  public static final String EXTRA_URI = "extra_uri";
  private static final int REQUEST_CODE_CAMERA = 75;
  private static final String EXTRA_OUTPUT = "extra_output";
  @Inject Preferences preferences;

  private File output;

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      output = (File) savedInstanceState.getSerializable(EXTRA_OUTPUT);
    } else {
      output = getFilename(".jpeg");
      if (output == null) {
        Toast.makeText(this, R.string.external_storage_unavailable, Toast.LENGTH_LONG).show();
      } else {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, output);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      if (resultCode == RESULT_OK) {
        if (output != null) {
          final Uri uri = Uri.fromFile(output);
          Intent intent = new Intent();
          intent.putExtra(EXTRA_URI, uri);
          setResult(RESULT_OK, intent);
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
      Timber.e(e);
    }
    return null;
  }
}
