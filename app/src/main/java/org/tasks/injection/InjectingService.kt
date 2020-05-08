package org.tasks.injection

import android.app.Notification
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.notifications.NotificationManager
import javax.inject.Inject

abstract class InjectingService : Service() {
    @Inject lateinit var firebase: Firebase
    private lateinit var disposables: CompositeDisposable

    override fun onCreate() {
        super.onCreate()
        startForeground()
        disposables = CompositeDisposable()
        inject((application as InjectingApplication).component.plus(ServiceModule()))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground()
        disposables.add(
                Completable.fromAction { doWork() }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ done(startId) }) { t: Throwable ->
                            firebase.reportException(t)
                            done(startId)
                        })
        return START_NOT_STICKY
    }

    private fun done(startId: Int) {
        scheduleNext()
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        disposables.dispose()
    }

    private fun startForeground() {
        startForeground(notificationId, buildNotification())
    }

    protected abstract val notificationId: Int
    protected abstract val notificationBody: Int

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(
                this, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
                .setSound(null)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(notificationBody))
                .build()
    }

    protected open fun scheduleNext() {}
    protected abstract fun doWork()
    protected abstract fun inject(component: ServiceComponent)
}