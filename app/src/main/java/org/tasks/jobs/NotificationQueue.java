package org.tasks.jobs;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Ints;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

@ApplicationScope
public class NotificationQueue {

  private final TreeMultimap<Long, NotificationQueueEntry> jobs =
      TreeMultimap.create(Ordering.natural(), (l, r) -> Ints.compare(l.hashCode(), r.hashCode()));
  private final Preferences preferences;
  private final WorkManager workManager;

  @Inject
  public NotificationQueue(Preferences preferences, WorkManager workManager) {
    this.preferences = preferences;
    this.workManager = workManager;
  }

  public synchronized <T extends NotificationQueueEntry> void add(T entry) {
    add(Collections.singletonList(entry));
  }

  public synchronized <T extends NotificationQueueEntry> void add(Iterable<T> entries) {
    long originalFirstTime = firstTime();
    for (T entry : filter(entries, notNull())) {
      jobs.put(entry.getTime(), entry);
    }
    if (originalFirstTime != firstTime()) {
      scheduleNext(true);
    }
  }

  public synchronized void clear() {
    jobs.clear();
    workManager.cancelNotifications();
  }

  public synchronized void cancelAlarm(long alarmId) {
    cancel(AlarmEntry.class, alarmId);
  }

  public synchronized void cancelReminder(long taskId) {
    cancel(ReminderEntry.class, taskId);
  }

  private void cancel(Class<? extends NotificationQueueEntry> c, long id) {
    long firstTime = firstTime();

    remove(newArrayList(filter(jobs.values(), r -> r.getClass().equals(c) && r.getId() == id)));

    if (firstTime != firstTime()) {
      scheduleNext(true);
    }
  }

  synchronized List<? extends NotificationQueueEntry> getOverdueJobs() {
    List<NotificationQueueEntry> result = newArrayList();
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
        workManager.cancelNotifications();
      }
    } else {
      workManager.scheduleNotification(nextScheduledTime());
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

  List<NotificationQueueEntry> getJobs() {
    return ImmutableList.copyOf(jobs.values());
  }

  public synchronized boolean remove(List<? extends NotificationQueueEntry> entries) {
    boolean success = true;
    for (NotificationQueueEntry entry : entries) {
      success &= !jobs.containsEntry(entry.getTime(), entry) || jobs.remove(entry.getTime(), entry);
    }
    return success;
  }
}
