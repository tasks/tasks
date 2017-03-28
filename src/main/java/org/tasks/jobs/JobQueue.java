package org.tasks.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Longs;

import org.tasks.preferences.Preferences;

import java.util.List;
import java.util.SortedSet;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class JobQueue<T extends JobQueueEntry> {
    private final TreeMultimap<Long, T> jobs = TreeMultimap.create(Ordering.natural(), (l, r) -> Longs.compare(l.getId(), r.getId()));
    private final Preferences preferences;

    public JobQueue(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean add(T entry) {
        boolean result = jobs.isEmpty() || entry.getTime() < firstTime();
        jobs.put(entry.getTime(), entry);
        return result;
    }

    public boolean isEmpty() {
        return jobs.isEmpty();
    }

    public void clear() {
        jobs.clear();
    }

    public boolean cancel(long id) {
        boolean reschedule = false;
        long firstTime = firstTime();
        List<T> existing = newArrayList(filter(jobs.values(), r -> r.getId() == id));
        for (T entry : existing) {
            reschedule |= entry.getTime() == firstTime;
            jobs.remove(entry.getTime(), entry);
        }
        return reschedule;
    }

    List<T> getJobs() {
        return ImmutableList.copyOf(jobs.values());
    }

    public List<T> removeOverdueJobs() {
        List<T> result = newArrayList();
        SortedSet<Long> lapsed = jobs.keySet().headSet(currentTimeMillis() + 1);
        for (Long key : lapsed) {
            result.addAll(jobs.removeAll(key));
        }
        return result;
    }

    public int size() {
        return jobs.size();
    }

    private long firstTime() {
        return jobs.isEmpty() ? 0 : jobs.asMap().firstKey();
    }

    public long nextScheduledTime() {
        long next = firstTime();
        return next > 0 ? preferences.adjustForQuietHours(next) : 0;
    }
}
