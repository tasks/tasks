package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;
import com.todoroo.astrid.service.TaskCompleter;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;
import timber.log.Timber;

public class CompleteTaskReceiver extends InjectingBroadcastReceiver {

  public static final String TASK_ID = "id";

  @Inject TaskCompleter taskCompleter;

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    long taskId = intent.getLongExtra(TASK_ID, 0);
    Timber.i("Completing %s", taskId);
    Completable.fromAction(() -> taskCompleter.setComplete(taskId))
        .subscribeOn(Schedulers.io())
        .subscribe();
  }

  @Override
  protected void inject(BroadcastComponent component) {
    component.inject(this);
  }
}
