package org.tasks.files;

import static org.tasks.PermissionUtil.verifyPermissions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import com.google.common.base.Strings;
import com.nononsenseapps.filepicker.FilePickerActivity;
import java.io.File;
import javax.inject.Inject;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;

public class FileExplore extends InjectingAppCompatActivity {

  public static final String EXTRA_FILE = "extra_file"; // $NON-NLS-1$
  public static final String EXTRA_DIRECTORY = "extra_directory"; // $NON-NLS-1$
  public static final String EXTRA_START_PATH = "extra_start_path";
  public static final String EXTRA_DIRECTORY_MODE = "extra_directory_mode"; // $NON-NLS-1$
  private static final int REQUEST_PICKER = 1000;
  @Inject ActivityPermissionRequestor permissionRequestor;

  private boolean directoryMode;
  private String startPath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      Intent intent = getIntent();
      directoryMode = intent.getBooleanExtra(EXTRA_DIRECTORY_MODE, false);
      startPath = intent.getStringExtra(EXTRA_START_PATH);

      if (permissionRequestor.requestFileWritePermission()) {
        launchPicker();
      }
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private void launchPicker() {
    File path = null;
    if (!Strings.isNullOrEmpty(startPath)) {
      path = new File(startPath);
    }
    if (path == null || !path.exists()) {
      if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        path = new File(Environment.getExternalStorageDirectory().toString());
      } else {
        path = Environment.getRootDirectory();
      }
    }

    Intent i = new Intent(this, MyFilePickerActivity.class);
    i.putExtra(FilePickerActivity.EXTRA_START_PATH, path.getAbsolutePath());
    if (directoryMode) {
      i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
      i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
    }
    startActivityForResult(i, REQUEST_PICKER);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_FILE_WRITE) {
      if (verifyPermissions(grantResults)) {
        launchPicker();
      } else {
        finish();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_PICKER) {
      if (resultCode == Activity.RESULT_OK) {
        Uri uri = data.getData();
        File file = com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
        Intent intent = new Intent();
        intent.putExtra(directoryMode ? EXTRA_DIRECTORY : EXTRA_FILE, file.getAbsolutePath());
        setResult(Activity.RESULT_OK, intent);
      }
      finish();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
