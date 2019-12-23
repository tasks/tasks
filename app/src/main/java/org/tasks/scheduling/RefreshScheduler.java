package org.tasks.scheduling;

import static com.todoroo.andlib.utility.DateUtilities.ONE_MINUTE;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.Preferences;

@ApplicationScope
public class RefreshScheduler {

  private final Preferences preferences;
  private final WorkManager workManager;
  private final TaskDao taskDao;
  private final SortedSet<Long> jobs = new TreeSet<>();

  @Inject
  RefreshScheduler(Preferences preferences, WorkManager workManager, TaskDao taskDao) {
    this.preferences = preferences;
    this.workManager = workManager;
    this.taskDao = taskDao;
  }

  public synchronized void scheduleAll() {
    for (Task task : taskDao.needsRefresh()) {
      scheduleRefresh(task);
    }
  }

  public synchronized void scheduleRefresh(Task task) {
    if (task.isCompleted()
        && preferences.getBoolean(R.string.p_temporarily_show_completed_tasks, false)) {
      scheduleRefresh(task.getCompletionDate() + ONE_MINUTE);
    } else if (task.hasDueDate()) {
      scheduleRefresh(task.getDueDate());
    }
    if (task.hasHideUntilDate()) {
      scheduleRefresh(task.getHideUntil());
    }
  }

  private void scheduleRefresh(Long timestamp) {
    long now = currentTimeMillis();
    if (now < timestamp) {
      SortedSet<Long> upcoming = jobs.tailSet(now);
      boolean reschedule = upcoming.isEmpty() || timestamp < upcoming.first();
      jobs.add(timestamp);
      if (reschedule) {
        scheduleNext();
      }
    }
  }

  public synchronized void scheduleNext() {
    List<Long> lapsed = ImmutableList.copyOf(jobs.headSet(currentTimeMillis() + 1));
    jobs.removeAll(lapsed);
    if (!jobs.isEmpty()) {
      workManager.scheduleRefresh(jobs.first());
    }
  }
}
