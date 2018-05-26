package org.tasks.injection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import javax.annotation.Nonnull;
import timber.log.Timber;

public abstract class InjectingJobIntentService extends JobIntentService {

  @Override
  protected final void onHandleWork(@NonNull Intent intent) {
    inject(
        ((InjectingApplication) getApplication()).getComponent().plus(new IntentServiceModule()));

    try {
      doWork(intent);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  protected abstract void doWork(@Nonnull Intent intent);

  protected abstract void inject(IntentServiceComponent component);
}
