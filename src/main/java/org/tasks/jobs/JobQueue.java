package org.tasks.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Longs;

import org.tasks.preferences.Preferences;

import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class JobQueue<T extends JobQueueEntry> {
    private final TreeMultimap<Long, T> jobs = TreeMultimap.create(Ordering.natural(), (l, r) -> Longs.compare(l.getId(), r.getId()));
    private final Preferences preferences;
    private final JobManager jobManager;
    private final String tag;

    public static JobQueue<Reminder> newReminderQueue(Preferences preferences, JobManager jobManager) {
        return new JobQueue<>(preferences, jobManager, ReminderJob.TAG);
    }

    public static JobQueue<Alarm> newAlarmQueue(Preferences preferences, JobManager jobManager) {
        return new JobQueue<>(preferences, jobManager, AlarmJob.TAG);
    }

    JobQueue(Preferences preferences, JobManager jobManager, String tag) {
        this.preferences = preferences;
        this.jobManager = jobManager;
        this.tag = tag;
    }

    public synchronized void add(T entry) {
        boolean reschedule = jobs.isEmpty() || entry.getTime() < firstTime();
        jobs.put(entry.getTime(), entry);
        if (reschedule) {
            scheduleNext(true);
        }
    }

    public synchronized void clear() {
        jobs.clear();
        jobManager.cancel(tag);
    }

    public synchronized void cancel(long id) {
        boolean reschedule = false;
        long firstTime = firstTime();
        List<T> existing = newArrayList(filter(jobs.values(), r -> r.getId() == id));
        for (T entry : existing) {
            reschedule |= entry.getTime() == firstTime;
            jobs.remove(entry.getTime(), entry);
        }
        if (reschedule) {
            scheduleNext(true);
        }
    }

    public synchronized List<T> getOverdueJobs() {
        List<T> result = newArrayList();
        for (Long key : jobs.keySet().headSet(currentTimeMillis() + 1)) {
            result.addAll(jobs.get(key));
        }
        return result;
    }

    public synchronized boolean remove(T entry) {
        return jobs.remove(entry.getTime(), entry);
    }

    public synchronized void scheduleNext() {
        scheduleNext(false);
    }

    private void scheduleNext(boolean cancelCurrent) {
        if (jobs.isEmpty()) {
            if (cancelCurrent) {
                jobManager.cancel(tag);
            }
        } else {
            jobManager.schedule(tag, nextScheduledTime());
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

    List<T> getJobs() {
        return ImmutableList.copyOf(jobs.values());
    }
}
