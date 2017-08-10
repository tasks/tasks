package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.JobManager;

import javax.inject.Inject;

import timber.log.Timber;

import static java.lang.System.currentTimeMillis;

public class SchedulerIntentService extends InjectingJobIntentService {

    @Inject TaskDao taskDao;
    @Inject JobManager jobManager;
    @Inject RefreshScheduler refreshScheduler;

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

        Timber.d("onHandleIntent(%s)", intent);

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
