package org.tasks.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import com.google.common.base.Strings;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import java.io.File;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

public class FileExplore extends InjectingAppCompatActivity {

  public static final String EXTRA_START_PATH = "extra_start_path";
  public static final String EXTRA_DIRECTORY_MODE = "extra_directory_mode"; // $NON-NLS-1$
  private static final int REQUEST_PICKER = 1000;

  private boolean directoryMode;
  private String startPath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      Intent intent = getIntent();
      directoryMode = intent.getBooleanExtra(EXTRA_DIRECTORY_MODE, false);
      startPath = intent.getStringExtra(EXTRA_START_PATH);

      launchPicker();
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_PICKER) {
      if (resultCode == Activity.RESULT_OK) {
        Intent intent = new Intent();
        File file = Utils.getFileForUri(data.getData());
        intent.setData(Uri.fromFile(file));
        setResult(Activity.RESULT_OK, intent);
      }
      finish();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
