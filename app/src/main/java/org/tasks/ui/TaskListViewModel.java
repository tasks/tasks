package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static io.reactivex.Single.fromCallable;
import static org.tasks.data.TaskListQuery.getQuery;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.paging.DataSource.Factory;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.sqlite.db.SimpleSQLiteQuery;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.data.SubtaskInfo;
import org.tasks.data.TaskContainer;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public class TaskListViewModel extends ViewModel implements Observer<PagedList<TaskContainer>> {

  private static final PagedList.Config PAGED_LIST_CONFIG =
      new PagedList.Config.Builder().setPageSize(20).build();

  @Inject Preferences preferences;
  @Inject TaskDao taskDao;
  private MutableLiveData<List<TaskContainer>> tasks = new MutableLiveData<>();
  private Filter filter;
  private boolean manualSortFilter;
  private final CompositeDisposable disposable = new CompositeDisposable();
  private LiveData<PagedList<TaskContainer>> internal;

  public void setFilter(@NonNull Filter filter) {
    if (!filter.equals(this.filter)
        || !filter.getSqlQuery().equals(this.filter.getSqlQuery())) {
      this.filter = filter;
      tasks = new MutableLiveData<>();
      invalidate();
    }
    manualSortFilter =
        (filter.supportsManualSort() && preferences.isManualSort())
            || (filter.supportsAstridSorting() && preferences.isAstridSort());
  }

  public void observe(LifecycleOwner owner, Observer<List<TaskContainer>> observer) {
    tasks.observe(owner, observer);
  }

  public void searchByFilter(Filter filter) {
    this.filter = filter;
    invalidate();
  }

  private void removeObserver() {
    if (internal != null) {
      internal.removeObserver(this);
    }
  }

  public void invalidate() {
    assertMainThread();

    removeObserver();

    if (filter == null) {
      return;
    }

    disposable.add(
        Single.fromCallable(taskDao::getSubtaskInfo)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                subtasks -> {
                  if (manualSortFilter || !preferences.usePagedQueries()) {
                    performNonPagedQuery(subtasks);
                  } else {
                    performPagedListQuery();
                  }
                },
                Timber::e));
  }

  private void performNonPagedQuery(SubtaskInfo subtasks) {
    disposable.add(
        fromCallable(() -> taskDao.fetchTasks(s -> getQuery(preferences, filter, s), subtasks))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(tasks::postValue, Timber::e));
  }

  private void performPagedListQuery() {
    List<String> queries = getQuery(preferences, filter, new SubtaskInfo());
    if (BuildConfig.DEBUG && queries.size() != 1) {
      throw new RuntimeException("Invalid queries");
    }
    SimpleSQLiteQuery query = new SimpleSQLiteQuery(queries.get(0));
    Timber.d("paged query: %s", query.getSql());
    Factory<Integer, TaskContainer> factory = taskDao.getTaskFactory(query);
    LivePagedListBuilder<Integer, TaskContainer> builder =
        new LivePagedListBuilder<>(factory, PAGED_LIST_CONFIG);
    List<TaskContainer> current = tasks.getValue();
    if (current instanceof PagedList) {
      Object lastKey = ((PagedList<TaskContainer>) current).getLastKey();
      if (lastKey instanceof Integer) {
        builder.setInitialLoadKey((Integer) lastKey);
      }
    }
    if (BuildConfig.DEBUG) {
      builder.setFetchExecutor(
          command ->
              Completable.fromAction(
                  () -> {
                    assertNotMainThread();
                    long start = now();
                    command.run();
                    Timber.d("*** paged list execution took %sms", now() - start);
                  })
                  .subscribeOn(Schedulers.io())
                  .subscribe());
    }
    internal = builder.build();
    internal.observeForever(this);
  }

  @Override
  protected void onCleared() {
    disposable.dispose();
    removeObserver();
  }

  public List<TaskContainer> getValue() {
    List<TaskContainer> value = tasks.getValue();
    return value != null ? value : Collections.emptyList();
  }

  @Override
  public void onChanged(PagedList<TaskContainer> taskContainers) {
    tasks.setValue(taskContainers);
  }
}
