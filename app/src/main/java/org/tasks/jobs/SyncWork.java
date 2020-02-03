package org.tasks.jobs;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static java.util.concurrent.Executors.newFixedThreadPool;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.analytics.Tracker;
import org.tasks.caldav.CaldavSynchronizer;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.etesync.EteSynchronizer;
import org.tasks.gtasks.GoogleTaskSynchronizer;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;

public class SyncWork extends InjectingWorker {

  private static final Object LOCK = new Object();

  @Inject CaldavSynchronizer caldavSynchronizer;
  @Inject EteSynchronizer eteSynchronizer;
  @Inject GoogleTaskSynchronizer googleTaskSynchronizer;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Preferences preferences;
  @Inject Tracker tracker;
  @Inject CaldavDao caldavDao;
  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject SyncAdapters syncAdapters;

  public SyncWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public Result run() {
    if (!syncAdapters.isSyncEnabled()) {
      return Result.success();
    }
    synchronized (LOCK) {
      if (preferences.isSyncOngoing()) {
        return Result.retry();
      }
    }
    preferences.setSyncOngoing(true);
    localBroadcastManager.broadcastRefresh();
    try {
      sync();
    } catch (Exception e) {
      tracker.reportException(e);
    } finally {
      preferences.setSyncOngoing(false);
      localBroadcastManager.broadcastRefresh();
    }
    return Result.success();
  }

  private void sync() throws InterruptedException {
    int numThreads = atLeastJellybeanMR1() ? Runtime.getRuntime().availableProcessors() : 2;
    ExecutorService executor = newFixedThreadPool(numThreads);

    for (CaldavAccount account : caldavDao.getAccounts()) {
      executor.execute(
          () -> {
            if (account.isCaldavAccount()) {
              caldavSynchronizer.sync(account);
            } else if (account.isEteSyncAccount()) {
              eteSynchronizer.sync(account);
            }
          });
    }
    List<GoogleTaskAccount> accounts = googleTaskListDao.getAccounts();
    for (int i = 0; i < accounts.size(); i++) {
      int count = i;
      executor.execute(() -> googleTaskSynchronizer.sync(accounts.get(count), count));
    }

    executor.shutdown();
    executor.awaitTermination(15, TimeUnit.MINUTES);
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
