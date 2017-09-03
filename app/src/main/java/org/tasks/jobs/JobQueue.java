package org.tasks.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Ints;

import org.tasks.injection.ApplicationScope;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

@ApplicationScope
public class JobQueue {
    private final TreeMultimap<Long, JobQueueEntry> jobs = TreeMultimap.create(Ordering.natural(), (l, r) -> Ints.compare(l.hashCode(), r.hashCode()));
    private final Preferences preferences;
    private final JobManager jobManager;

    @Inject
    public JobQueue(Preferences preferences, JobManager jobManager) {
        this.preferences = preferences;
        this.jobManager = jobManager;
    }

    public synchronized <T extends JobQueueEntry> void add(T entry) {
        boolean reschedule = jobs.isEmpty() || entry.getTime() < firstTime();
        jobs.put(entry.getTime(), entry);
        if (reschedule) {
            scheduleNext(true);
        }
    }

    public synchronized void clear() {
        jobs.clear();
        jobManager.cancel(NotificationJob.TAG);
    }

    public synchronized void cancelAlarm(long alarmId) {
        cancel(Alarm.class, alarmId);
    }

    public synchronized void cancelReminder(long taskId) {
        cancel(Reminder.class, taskId);
    }

    private synchronized void cancel(Class<? extends JobQueueEntry> c, long id) {
        boolean reschedule = false;
        long firstTime = firstTime();
        List<JobQueueEntry> existing = newArrayList(
                filter(jobs.values(), r -> r.getClass().equals(c) && r.getId() == id));
        for (JobQueueEntry entry : existing) {
            reschedule |= entry.getTime() == firstTime;
            jobs.remove(entry.getTime(), entry);
        }
        if (reschedule) {
            scheduleNext(true);
        }
    }

    synchronized List<? extends JobQueueEntry> getOverdueJobs() {
        List<JobQueueEntry> result = newArrayList();
        long cutoff = new DateTime().startOfMinute().plusMinutes(1).getMillis();
        for (Long key : jobs.keySet().headSet(cutoff)) {
            result.addAll(jobs.get(key));
        }
        return result;
    }

    synchronized void scheduleNext() {
        scheduleNext(false);
    }

    private void scheduleNext(boolean cancelCurrent) {
        if (jobs.isEmpty()) {
            if (cancelCurrent) {
                jobManager.cancel(NotificationJob.TAG);
            }
        } else {
            jobManager.schedule(NotificationJob.TAG, nextScheduledTime());
        }
    }

    private long firstTime() {
        return jobs.isEmpty() ? 0 : jobs.asMap().firstKey();
    }

    long nextScheduledTime() {
        long next = firstTime();
        return next > 0 ? preferences.adjustForQuietHours(next) : 0;
    }

    int size() {
        return jobs.size();
    }

    List<JobQueueEntry> getJobs() {
        return ImmutableList.copyOf(jobs.values());
    }

    public synchronized boolean remove(List<? extends JobQueueEntry> entries) {
        boolean success = true;
        for (JobQueueEntry entry : entries) {
            success &= jobs.remove(entry.getTime(), entry);
        }
        return success;
    }
}
