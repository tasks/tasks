/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.alarms;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.AlarmJob;
import org.tasks.jobs.JobQueue;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 */
@ApplicationScope
public class AlarmService {

  private static final long NO_ALARM = Long.MAX_VALUE;

  private final JobQueue jobs;
  private final AlarmDao alarmDao;

  @Inject
  public AlarmService(AlarmDao alarmDao, JobQueue jobQueue) {
    this.alarmDao = alarmDao;
    jobs = jobQueue;
  }

  public void rescheduleAlarms(long taskId, long oldDueDate, long newDueDate) {
    if (newDueDate <= 0 || newDueDate <= oldDueDate) {
      return;
    }

    final Set<Long> alarms = new LinkedHashSet<>();
    for (Alarm alarm : getAlarms(taskId)) {
      alarms.add(alarm.getTime() + (newDueDate - oldDueDate));
    }
    if (!alarms.isEmpty()) {
      synchronizeAlarms(taskId, alarms);
    }
  }

  public List<org.tasks.data.Alarm> getAlarms(long taskId) {
    return alarmDao.getAlarms(taskId);
  }

  /**
   * Save the given array of alarms into the database
   *
   * @return true if data was changed
   */
  public boolean synchronizeAlarms(final long taskId, Set<Long> timestamps) {
    boolean changed = false;

    for (Alarm item : alarmDao.getAlarms(taskId)) {
      if (!timestamps.contains(item.getTime())) {
        jobs.cancelAlarm(item.getId());
        alarmDao.delete(item);
        changed = true;
      }
    }

    // everything that remains shall be written
    for (Long timestamp : timestamps) {
      alarmDao.insert(new Alarm(taskId, timestamp));
      changed = true;
    }

    if (changed) {
      scheduleAlarms(taskId);
    }
    return changed;
  }

  // --- alarm scheduling

  private List<org.tasks.data.Alarm> getActiveAlarms() {
    return alarmDao.getActiveAlarms();
  }

  private List<org.tasks.data.Alarm> getActiveAlarmsForTask(long taskId) {
    return alarmDao.getActiveAlarms(taskId);
  }

  /**
   * Schedules all alarms
   */
  public void scheduleAllAlarms() {
    for (Alarm alarm : getActiveAlarms()) {
      scheduleAlarm(alarm);
    }
  }

  /**
   * Schedules alarms for a single task
   */
  private void scheduleAlarms(long taskId) {
    for (Alarm alarm : getActiveAlarmsForTask(taskId)) {
      scheduleAlarm(alarm);
    }
  }

  /**
   * Schedules alarms for a single task
   */
  private void scheduleAlarm(Alarm alarm) {
    if (alarm == null) {
      return;
    }

    AlarmJob alarmJob = new AlarmJob(alarm);
    long time = alarmJob.getTime();
    if (time == 0 || time == NO_ALARM) {
      jobs.cancelAlarm(alarmJob.getId());
    } else {
      jobs.add(alarmJob);
    }
  }
}
