package org.tasks.scheduling;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.DateUtilities.ONE_MINUTE;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import com.todoroo.astrid.data.Task;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.JobManager;

@ApplicationScope
public class RefreshScheduler {

  private final JobManager jobManager;
  private final SortedSet<Long> jobs = new TreeSet<>();

  @Inject
  public RefreshScheduler(JobManager jobManager) {
    this.jobManager = jobManager;
  }

  public void scheduleRefresh(Task task) {
    if (task.isCompleted()) {
      scheduleRefresh(task.getCompletionDate() + ONE_MINUTE);
    } else if (task.hasDueDate()) {
      scheduleRefresh(task.getDueDate());
    }
    if (task.hasHideUntilDate()) {
      scheduleRefresh(task.getHideUntil());
    }
  }

  private void scheduleRefresh(Long refreshTime) {
    long now = currentTimeMillis();
    if (now < refreshTime) {
      refreshTime += 1000; // this is ghetto
      schedule(refreshTime);
    }
  }

  private void schedule(long timestamp) {
    SortedSet<Long> upcoming = jobs.tailSet(currentTimeMillis());
    boolean reschedule = upcoming.isEmpty() || timestamp < upcoming.first();
    jobs.add(timestamp);
    if (reschedule) {
      scheduleNext();
    }
  }

  public void scheduleNext() {
    long now = currentTimeMillis();
    jobs.removeAll(newArrayList(jobs.headSet(now + 1)));
    if (!jobs.isEmpty()) {
      jobManager.scheduleRefresh(jobs.first());
    }
  }
}
