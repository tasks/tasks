package org.tasks.injection;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.notifications.NotificationManager;

public abstract class InjectingService extends Service {

  @Inject Tracker tracker;

  private CompositeDisposable disposables;

  @Override
  public void onCreate() {
    super.onCreate();

    startForeground();

    disposables = new CompositeDisposable();

    inject(((InjectingApplication) getApplication()).getComponent().plus(new ServiceModule()));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startForeground();

    disposables.add(
        Completable.fromAction(this::doWork)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> done(startId),
                t -> {
                  tracker.reportException(t);
                  done(startId);
                }));

    return Service.START_NOT_STICKY;
  }

  private void done(int startId) {
    scheduleNext();
    stopSelf(startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopForeground(true);

    disposables.dispose();
  }

  private void startForeground() {
    startForeground(getNotificationId(), buildNotification());
  }

  protected abstract int getNotificationId();

  protected abstract int getNotificationBody();

  private Notification buildNotification() {
    return new NotificationCompat.Builder(
            this, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
        .setSound(null)
        .setSmallIcon(R.drawable.ic_check_white_24dp)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(getNotificationBody()))
        .build();
  }

  protected void scheduleNext() {

  }

  protected abstract void doWork();

  protected abstract void inject(ServiceComponent component);
}
