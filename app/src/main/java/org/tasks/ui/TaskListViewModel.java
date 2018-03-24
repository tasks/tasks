package org.tasks.ui;

import static com.todoroo.astrid.activity.TaskListFragment.FILE_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.TAGS_METADATA_JOIN;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.LivePagedListProvider;
import android.arch.paging.PagedList;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.data.LimitOffsetDataSource;
import org.tasks.data.Tag;
import org.tasks.data.TaskAttachment;
import org.tasks.preferences.Preferences;

public class TaskListViewModel extends ViewModel {

  @Inject TaskDao taskDao;
  @Inject Preferences preferences;

  private LimitOffsetDataSource latest;
  private LiveData<PagedList<Task>> tasks;
  private Filter filter;

  public void clear() {
    tasks = null;
    latest = null;
    filter = null;
  }

  public LiveData<PagedList<Task>> getTasks(Filter filter, Property<?>[] properties) {
    if (tasks == null || !filter.equals(this.filter)) {
      this.filter = filter;
      tasks = getLiveData(filter, properties);
    }
    return tasks;
  }

  private LiveData<PagedList<Task>> getLiveData(Filter filter, Property<?>[] properties) {
    return new LivePagedListBuilder<>(
        new LivePagedListProvider<Integer, Task>() {
          @Override
          protected LimitOffsetDataSource createDataSource() {
            latest = toDataSource(filter, properties);
            return latest;
          }
        }, 20)
        .build();
  }

  private LimitOffsetDataSource toDataSource(Filter filter, Property<?>[] properties) {
    Criterion tagsJoinCriterion = Criterion.and(
        Task.ID.eq(Field.field(TAGS_METADATA_JOIN + ".task")));

    if (filter instanceof TagFilter) {
      String uuid = ((TagFilter) filter).getUuid();
      tagsJoinCriterion = Criterion
          .and(tagsJoinCriterion, Field.field(TAGS_METADATA_JOIN + ".tag_uid").neq(uuid));
    }

    // TODO: For now, we'll modify the query to join and include the things like tag data here.
    // Eventually, we might consider restructuring things so that this query is constructed elsewhere.
    String joinedQuery =
        Join.left(Tag.TABLE.as(TAGS_METADATA_JOIN),
            tagsJoinCriterion).toString() //$NON-NLS-1$
            + Join.left(TaskAttachment.TABLE.as(FILE_METADATA_JOIN),
            Task.UUID.eq(Field.field(FILE_METADATA_JOIN + ".task_id")))
            + filter.getSqlQuery();

    String query = SortHelper.adjustQueryForFlagsAndSort(
        preferences, joinedQuery, preferences.getSortMode());

    String groupedQuery;
    if (query.contains("GROUP BY")) {
      groupedQuery = query;
    } else if (query.contains("ORDER BY")) {
      groupedQuery = query.replace("ORDER BY", "GROUP BY " + Task.ID + " ORDER BY"); //$NON-NLS-1$
    } else {
      groupedQuery = query + " GROUP BY " + Task.ID;
    }

    return taskDao.getLimitOffsetDataSource(groupedQuery, properties);
  }

  public void invalidate() {
    if (latest != null) {
      latest.invalidate();
    }
  }
}
