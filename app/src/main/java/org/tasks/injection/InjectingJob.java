package org.tasks.injection;

import android.support.annotation.NonNull;
import com.evernote.android.job.Job;

public abstract class InjectingJob extends Job {

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    JobComponent component =
        Dagger.get(getContext()).getApplicationComponent().plus(new JobModule());
    inject(component);
    return Result.SUCCESS;
  }

  protected abstract void inject(JobComponent component);
}
