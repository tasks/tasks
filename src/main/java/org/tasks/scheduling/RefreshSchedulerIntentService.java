package org.tasks.scheduling;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.Broadcaster;
import org.tasks.injection.IntentServiceComponent;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;

public class RefreshSchedulerIntentService extends MidnightIntentService {

    @Inject Broadcaster broadcaster;
    @Inject RefreshScheduler refreshScheduler;
    @Inject TaskDao taskDao;

    public RefreshSchedulerIntentService() {
        super(RefreshSchedulerIntentService.class.getSimpleName());
    }

    @Override
    void run() {
        scheduleApplicationRefreshes();
        broadcaster.refresh();
    }

    public void scheduleApplicationRefreshes() {
        long now = currentTimeMillis();
        long midnight = nextMidnight(now);
        Criterion criterion = Criterion.or(
                Criterion.and(Task.HIDE_UNTIL.gt(now), Task.HIDE_UNTIL.lt(midnight)),
                Criterion.and(Task.DUE_DATE.gt(now), Task.DUE_DATE.lt(midnight)));
        taskDao.selectActive(criterion, refreshScheduler::scheduleRefresh);
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
