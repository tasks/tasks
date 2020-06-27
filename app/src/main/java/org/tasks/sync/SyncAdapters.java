package org.tasks.sync;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.data.CaldavDaoBlocking;
import org.tasks.data.GoogleTaskListDaoBlocking;
import org.tasks.jobs.WorkManager;

public class SyncAdapters {

  private final WorkManager workManager;
  private final CaldavDaoBlocking caldavDao;
  private final GoogleTaskListDaoBlocking googleTaskListDao;

  @Inject
  public SyncAdapters(
      WorkManager workManager, CaldavDaoBlocking caldavDao, GoogleTaskListDaoBlocking googleTaskListDao) {
    this.workManager = workManager;
    this.caldavDao = caldavDao;
    this.googleTaskListDao = googleTaskListDao;
  }

  public void sync() {
    sync(false).subscribe();
  }

  public Single<Boolean> sync(boolean immediate) {
    return Single.fromCallable(this::isSyncEnabled)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(enabled -> {
          if (enabled) {
            workManager.sync(immediate);
          }
        });
  }

  public boolean isSyncEnabled() {
    return isGoogleTaskSyncEnabled() || isCaldavSyncEnabled();
  }

  public boolean isGoogleTaskSyncEnabled() {
    return googleTaskListDao.getAccounts().size() > 0;
  }

  public boolean isCaldavSyncEnabled() {
    return caldavDao.accountCount() > 0;
  }
}
