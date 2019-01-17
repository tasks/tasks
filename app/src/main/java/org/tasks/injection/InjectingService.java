package org.tasks.injection;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.annotation.Nonnull;

public abstract class InjectingService extends Service {

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startForeground(getNotificationId(), getNotification());

    inject(((InjectingApplication) getApplication()).getComponent().plus(new ServiceModule()));

    Completable.fromAction(() -> doWork(intent))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .doFinally(this::stop)
        .subscribe();

    return Service.START_NOT_STICKY;
  }

  private void stop() {
    stopForeground(true);
    stopSelf();
  }

  protected abstract int getNotificationId();

  protected abstract Notification getNotification();

  protected abstract void doWork(@Nonnull Intent intent);

  protected abstract void inject(ServiceComponent component);
}
