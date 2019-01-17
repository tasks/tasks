package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.jobs.AfterSaveWork;
import org.tasks.jobs.BackupWork;
import org.tasks.jobs.CleanupWork;
import org.tasks.jobs.DriveUploader;
import org.tasks.jobs.MidnightRefreshWork;
import org.tasks.jobs.RefreshWork;
import org.tasks.jobs.SyncWork;

@Subcomponent(modules = WorkModule.class)
public interface JobComponent {

  void inject(SyncWork syncWork);

  void inject(BackupWork backupWork);

  void inject(RefreshWork refreshWork);

  void inject(CleanupWork cleanupWork);

  void inject(MidnightRefreshWork midnightRefreshWork);

  void inject(AfterSaveWork afterSaveWork);

  void inject(DriveUploader driveUploader);
}
