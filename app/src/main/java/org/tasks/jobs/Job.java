package org.tasks.jobs;

import android.content.Intent;
import android.support.annotation.NonNull;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingJobIntentService;
import timber.log.Timber;

public abstract class Job extends InjectingJobIntentService {

  @Inject Tracker tracker;

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    super.onHandleWork(intent);

    Timber.d("onHandleIntent(%s)", intent);

    try {
      run();
    } catch (Exception e) {
      tracker.reportException(e);
    } finally {
      scheduleNext();
    }
  }

  protected abstract void run();

  protected abstract void scheduleNext();
}
