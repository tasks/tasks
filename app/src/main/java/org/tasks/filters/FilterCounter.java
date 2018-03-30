package org.tasks.filters;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class FilterCounter {

  // Previous solution involved a queue of filters and a filterSizeLoadingThread. The
  // filterSizeLoadingThread had
  // a few problems: how to make sure that the thread is resumed when the controlling activity is
  // resumed, and
  // how to make sure that the the filterQueue does not accumulate filters without being processed.
  // I am replacing
  // both the queue and a the thread with a thread pool, which will shut itself off after a second
  // if it has
  // nothing to do (corePoolSize == 0, which makes it available for garbage collection), and will
  // wake itself up
  // if new filters are queued (obviously it cannot be garbage collected if it is possible for new
  // filters to
  // be added).
  private final ExecutorService executorService;

  private final Map<Filter, Integer> filterCounts = new ConcurrentHashMap<>();

  private final TaskDao taskDao;

  @Inject
  public FilterCounter(TaskDao taskDao) {
    this(taskDao, new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
  }

  private FilterCounter(TaskDao taskDao, ExecutorService executorService) {
    this.taskDao = taskDao;
    this.executorService = executorService;
  }

  public void refreshFilterCounts(final Runnable onComplete) {
    executorService.submit(
        () -> {
          for (Filter filter : filterCounts.keySet()) {
            int size = taskDao.count(filter);
            filterCounts.put(filter, size);
          }
          if (onComplete != null) {
            onComplete.run();
          }
        });
  }

  public void registerFilter(Filter filter) {
    if (!filterCounts.containsKey(filter)) {
      filterCounts.put(filter, 0);
    }
  }

  public boolean containsKey(FilterListItem filter) {
    return filterCounts.containsKey(filter);
  }

  public Integer get(FilterListItem filter) {
    return filterCounts.get(filter);
  }
}
