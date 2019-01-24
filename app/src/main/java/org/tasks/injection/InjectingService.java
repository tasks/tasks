package org.tasks.injection;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.tasks.R;
import org.tasks.notifications.NotificationManager;

public abstract class InjectingService extends Service {

  private CompositeDisposable disposables = new CompositeDisposable();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startForeground(getNotificationId(), buildNotification());

    inject(((InjectingApplication) getApplication()).getComponent().plus(new ServiceModule()));

    disposables.add(
        Completable.fromAction(this::doWork)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::stopSelf));

    return Service.START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopForeground(true);

    disposables.dispose();
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

  protected abstract void doWork();

  protected abstract void inject(ServiceComponent component);
}
