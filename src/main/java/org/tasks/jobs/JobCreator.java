package org.tasks.jobs;

import android.content.Context;

import com.evernote.android.job.Job;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.backup.TasksXmlExporter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Broadcaster;
import org.tasks.Notifier;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.RefreshScheduler;

import javax.inject.Inject;

@ApplicationScope
public class JobCreator implements com.evernote.android.job.JobCreator {

    private final Context context;
    private final Notifier notifier;
    private final JobManager jobManager;
    private final Broadcaster broadcaster;
    private final TasksXmlExporter tasksXmlExporter;
    private final Preferences preferences;
    private final RefreshScheduler refreshScheduler;
    private final AlarmService alarmService;
    private final TaskDao taskDao;
    private final ReminderService reminderService;

    @Inject
    public JobCreator(@ForApplication Context context, Notifier notifier, JobManager jobManager,
                      Broadcaster broadcaster, TasksXmlExporter tasksXmlExporter,
                      Preferences preferences, RefreshScheduler refreshScheduler,
                      AlarmService alarmService, TaskDao taskDao, ReminderService reminderService) {
        this.context = context;
        this.notifier = notifier;
        this.jobManager = jobManager;
        this.broadcaster = broadcaster;
        this.tasksXmlExporter = tasksXmlExporter;
        this.preferences = preferences;
        this.refreshScheduler = refreshScheduler;
        this.alarmService = alarmService;
        this.taskDao = taskDao;
        this.reminderService = reminderService;
    }

    @Override
    public Job create(String tag) {
        switch (tag) {
            case ReminderJob.TAG:
                return new ReminderJob(preferences, reminderService, notifier);
            case AlarmJob.TAG:
                return new AlarmJob(preferences, alarmService, notifier, taskDao);
            case RefreshJob.TAG:
                return new RefreshJob(refreshScheduler, broadcaster);
            case MidnightRefreshJob.TAG:
                return new MidnightRefreshJob(broadcaster, jobManager);
            case BackupJob.TAG:
                return new BackupJob(context, jobManager, tasksXmlExporter, preferences);
            default:
                return null;
        }
    }
}
