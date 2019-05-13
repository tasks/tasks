package org.tasks.sync;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.jobs.WorkManager;

public class SyncAdapters {

  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final WorkManager workManager;
  private final CaldavDao caldavDao;

  @Inject
  public SyncAdapters(
      GtaskSyncAdapterHelper gtaskSyncAdapterHelper, WorkManager workManager, CaldavDao caldavDao) {
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.workManager = workManager;
    this.caldavDao = caldavDao;
  }

  public void sync() {
    sync(false).subscribe();
  }

  public Single<Boolean> sync(boolean immediate) {
    return Single.zip(
            Single.fromCallable(this::isGoogleTaskSyncEnabled),
            Single.fromCallable(this::isCaldavSyncEnabled),
            (b1, b2) -> {
              if (b1 || b2) {
                workManager.sync(immediate);
                return true;
              }
              return false;
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public boolean isSyncEnabled() {
    return isGoogleTaskSyncEnabled() || isCaldavSyncEnabled();
  }

  public boolean isGoogleTaskSyncEnabled() {
    return gtaskSyncAdapterHelper.isEnabled();
  }

  public boolean isCaldavSyncEnabled() {
    return caldavDao.getAccounts().size() > 0;
  }
}
