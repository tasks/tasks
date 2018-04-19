package org.tasks.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.evernote.android.job.Job;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import timber.log.Timber;

@ApplicationScope
public class JobCreator implements com.evernote.android.job.JobCreator {

  static final String TAG_BACKUP = "tag_backup";
  static final String TAG_REFRESH = "tag_refresh";
  static final String TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh";
  static final String TAG_NOTIFICATION = "tag_notification";
  static final String TAG_BACKGROUND_SYNC = "tag_background_sync";
  static final String TAG_SYNC = "tag_sync";
  static final String TAG_CLEANUP = "tag_cleanup";

  @Inject
  public JobCreator() {}

  @Nullable
  @Override
  public Job create(@NonNull String tag) {
    switch (tag) {
      case TAG_NOTIFICATION:
        return new NotificationJob();
      case TAG_SYNC:
      case TAG_BACKGROUND_SYNC:
        return new SyncJob();
      case TAG_BACKUP:
        return new BackupJob();
      case TAG_MIDNIGHT_REFRESH:
      case TAG_REFRESH:
        return new RefreshJob();
      case TAG_CLEANUP:
        return new CleanupJob();
      default:
        Timber.e("Unhandled tag: %s", tag);
        return null;
    }
  }
}
