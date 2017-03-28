package com.todoroo.astrid.reminders;

import com.todoroo.astrid.data.Task;

import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.JobManager;
import org.tasks.jobs.JobQueue;
import org.tasks.jobs.Reminder;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;

import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;

@ApplicationScope
public class ReminderAlarmScheduler implements AlarmScheduler {

    private final JobQueue<Reminder> jobs;
    private final JobManager jobManager;

    @Inject
    public ReminderAlarmScheduler(JobManager jobManager, Preferences preferences) {
        this.jobManager = jobManager;
        jobs = new JobQueue<>(preferences);
    }

    /**
     * Create an alarm for the given task at the given type
     */
    @Override
    public void createAlarm(Task task, long time, int type) {
        long taskId = task.getId();
        if(taskId == Task.NO_ID) {
            return;
        }

        if (time == 0 || time == NO_ALARM) {
            if (jobs.cancel(taskId)) {
                scheduleNext(true);
            }
        } else {
            Reminder reminder = new Reminder(taskId, time, type);
            if (jobs.add(reminder)) {
                scheduleNext(true);
            }
        }
    }

    @Override
    public void clear() {
        jobs.clear();
        jobManager.cancelReminders();
    }

    public void scheduleNextJob() {
        scheduleNext(false);
    }

    private void scheduleNext(boolean cancelCurrent) {
        if (jobs.isEmpty()) {
            if (cancelCurrent) {
                jobManager.cancelReminders();
            }
        } else {
            jobManager.scheduleReminder(jobs.nextScheduledTime(), cancelCurrent);
        }
    }

    public List<Reminder> removePastReminders() {
        return jobs.removeOverdueJobs();
    }
}
