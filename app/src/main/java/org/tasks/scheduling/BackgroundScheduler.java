package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.JobManager;

import javax.inject.Inject;

import timber.log.Timber;

import static java.lang.System.currentTimeMillis;

public class BackgroundScheduler extends InjectingJobIntentService {

    public static void enqueueWork(Context context) {
        BackgroundScheduler.enqueueWork(context, BackgroundScheduler.class, JobManager.JOB_ID_BACKGROUND_SCHEDULER, new Intent());
    }

    @Inject @ForApplication Context context;
    @Inject TaskDao taskDao;
    @Inject JobManager jobManager;
    @Inject RefreshScheduler refreshScheduler;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        super.onHandleWork(intent);

        Timber.d("onHandleWork(%s)", intent);

        NotificationSchedulerIntentService.enqueueWork(context, false);
        CalendarNotificationIntentService.enqueueWork(context);
        GeofenceSchedulingIntentService.enqueueWork(context);

        jobManager.scheduleMidnightBackup();
        jobManager.scheduleMidnightRefresh();

        refreshScheduler.clear();
        long now = currentTimeMillis();
        taskDao.selectActive(
                Criterion.or(Task.HIDE_UNTIL.gt(now), Task.DUE_DATE.gt(now)),
                refreshScheduler::scheduleRefresh);
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
