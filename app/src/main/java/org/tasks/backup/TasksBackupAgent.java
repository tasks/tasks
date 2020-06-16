package org.tasks.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.FileBackupHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.todoroo.astrid.backup.BackupConstants;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.components.ApplicationComponent;
import java.io.File;
import java.io.IOException;
import timber.log.Timber;

public class TasksBackupAgent extends BackupAgentHelper {

  @EntryPoint
  @InstallIn(ApplicationComponent.class)
  interface TasksBackupAgentEntryPoint {
    TasksJsonImporter getTasksJsonImporter();
  }

  private static final String BACKUP_KEY = "backup";

  private TasksJsonImporter importer;

  @Override
  public void onCreate() {
    TasksBackupAgentEntryPoint hilt =
        EntryPointAccessors.fromApplication(getApplicationContext(), TasksBackupAgentEntryPoint.class);
    importer = hilt.getTasksJsonImporter();

    addHelper(BACKUP_KEY, new FileBackupHelper(this, BackupConstants.INTERNAL_BACKUP));
  }

  @Override
  public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
      throws IOException {
    super.onRestore(data, appVersionCode, newState);

    File backup =
        new File(
            String.format(
                "%s/%s", getFilesDir().getAbsolutePath(), BackupConstants.INTERNAL_BACKUP));

    if (backup.exists()) {
      importer.importTasks(this, Uri.fromFile(backup), null);
    } else {
      Timber.w("%s not found", backup.getAbsolutePath());
    }
  }

  @Override
  public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
    Timber.e("onQuotaExceeded(%s, %s)", backupDataBytes, quotaBytes);
  }
}
