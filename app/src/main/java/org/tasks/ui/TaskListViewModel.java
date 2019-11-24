package org.tasks.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.sql.Field.field;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.astrid.activity.TaskListFragment.CALDAV_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.GTASK_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.TAGS_METADATA_JOIN;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavTask;
import org.tasks.data.Geofence;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.Place;
import org.tasks.data.Tag;
import org.tasks.data.TaskContainer;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class TaskListViewModel extends ViewModel {

  private static final Table RECURSIVE = new Table("recursive_tasks");
  private static final Field RECURSIVE_TASK = field(RECURSIVE + ".task");
  private static final Field TASKS = field("tasks.*");
  private static final Field GTASK = field(GTASK_METADATA_JOIN + ".*");
  private static final Field GEOFENCE = field("geofences.*");
  private static final Field PLACE = field("places.*");
  private static final Field CALDAV = field(CALDAV_METADATA_JOIN + ".*");
  private static final Field CHILDREN = field("children");
  private static final Field SIBLINGS = field("siblings");
  private static final Field PRIMARY_SORT = field("primary_sort").as("primarySort");
  private static final Field SECONDARY_SORT = field("secondary_sort").as("secondarySort");
  private static final Field INDENT = field("indent");
  private static final StringProperty TAGS =
      new StringProperty(null, "group_concat(distinct(" + TAGS_METADATA_JOIN + ".tag_uid)" + ")")
          .as("tags");
  private static final StringProperty TAGS_RECURSIVE =
      new StringProperty(null, "(SELECT group_concat(distinct(tag_uid))\n" +
              "FROM tags WHERE tags.task = tasks._id\n" +
              "GROUP BY tags.task)")
          .as("tags");

  @Inject Preferences preferences;
  @Inject TaskDao taskDao;
  private MutableLiveData<List<TaskContainer>> tasks = new MutableLiveData<>();
  private Filter filter;
  private boolean manualSort;
  private CompositeDisposable disposable = new CompositeDisposable();

  public void setFilter(@NonNull Filter filter, boolean manualSort) {
    if (!filter.equals(this.filter)
        || !filter.getSqlQuery().equals(this.filter.getSqlQuery())
        || this.manualSort != manualSort) {
      this.filter = filter;
      this.manualSort = manualSort;
      tasks = new MutableLiveData<>();
      invalidate();
    }
  }

  public void observe(LifecycleOwner owner, Observer<List<TaskContainer>> observer) {
    tasks.observe(owner, observer);
  }

  public static List<String> getQuery(Preferences preferences, Filter filter) {
    List<Field> fields = newArrayList(TASKS, GTASK, CALDAV, GEOFENCE, PLACE);

    Criterion tagsJoinCriterion = Criterion.and(Task.ID.eq(field(TAGS_METADATA_JOIN + ".task")));
    Criterion gtaskJoinCriterion =
        Criterion.and(
            Task.ID.eq(field(GTASK_METADATA_JOIN + ".gt_task")),
            field(GTASK_METADATA_JOIN + ".gt_deleted").eq(0));
    Criterion caldavJoinCriterion =
        Criterion.and(
            Task.ID.eq(field(CALDAV_METADATA_JOIN + ".cd_task")),
            field(CALDAV_METADATA_JOIN + ".cd_deleted").eq(0));
    if (filter instanceof TagFilter) {
      String uuid = ((TagFilter) filter).getUuid();
      tagsJoinCriterion =
          Criterion.and(tagsJoinCriterion, field(TAGS_METADATA_JOIN + ".tag_uid").neq(uuid));
    } else if (filter instanceof GtasksFilter) {
      if (preferences.isManualSort()) {
        fields.add(INDENT);
        fields.add(CHILDREN);
        fields.add(SIBLINGS);
        fields.add(PRIMARY_SORT);
        fields.add(SECONDARY_SORT);
      }
    } else if (filter instanceof CaldavFilter) {
      String uuid = ((CaldavFilter) filter).getUuid();
      caldavJoinCriterion =
          Criterion.and(caldavJoinCriterion, field(CALDAV_METADATA_JOIN + ".cd_calendar").eq(uuid));
    }

    String joins =
        Join.left(GoogleTask.TABLE.as(GTASK_METADATA_JOIN), gtaskJoinCriterion).toString()
            + Join.left(CaldavTask.TABLE.as(CALDAV_METADATA_JOIN), caldavJoinCriterion)
            + Join.left(Geofence.TABLE, Geofence.TASK.eq(Task.ID))
            + Join.left(Place.TABLE, Place.UID.eq(Geofence.PLACE));

    if (atLeastLollipop()
        && (filter instanceof CaldavFilter
            || (!preferences.isManualSort() && filter instanceof GtasksFilter))) {
      // TODO This is in some ways a proof of concept demonstrating a recursive query used to pull
      //      in CalDAV tasks providing parenting across different sort modes. Tags are implemented
      //      as a subquery, which is ugly, but aggregate recursive queries aren't supported. The
      //      link to eg. GoogleTasks remains as it was originally explored as a drop-in replacement
      //      for the main query. Need to verify the approach and look at how this can be applied
      //      across backends plus investigate integrating more closely with the query-building
      //      classes in the architecture.

      fields.add(TAGS_RECURSIVE);
      fields.add(INDENT);

      String joinedQuery = Join.inner(RECURSIVE, Task.ID.eq(RECURSIVE_TASK)) + joins;
      String parentQuery;
      QueryTemplate subtaskQuery = new QueryTemplate();
      if (filter instanceof CaldavFilter) {
        CaldavCalendar calendar = ((CaldavFilter) filter).getCalendar();
        parentQuery =
            new QueryTemplate()
                .join(
                    Join.inner(
                        CaldavTask.TABLE,
                        Criterion.and(
                            CaldavTask.CALENDAR.eq(calendar.getUuid()),
                            CaldavTask.PARENT.eq(0),
                            CaldavTask.TASK.eq(Task.ID),
                            CaldavTask.DELETED.eq(0))))
                .where(TaskCriteria.activeAndVisible())
                .toString();
        subtaskQuery
            .join(Join.inner(RECURSIVE, CaldavTask.PARENT.eq(RECURSIVE_TASK)))
            .join(
                Join.inner(
                    CaldavTask.TABLE,
                    Criterion.and(
                        CaldavTask.CALENDAR.eq(calendar.getUuid()),
                        CaldavTask.PARENT.gt(0),
                        CaldavTask.TASK.eq(Task.ID),
                        CaldavTask.DELETED.eq(0))))
            .where(TaskCriteria.activeAndVisible());
      } else {
        GoogleTaskList list = ((GtasksFilter) filter).getList();
        parentQuery =
            new QueryTemplate()
                .join(
                    Join.inner(
                        GoogleTask.TABLE,
                        Criterion.and(
                            GoogleTask.LIST.eq(list.getRemoteId()),
                            GoogleTask.PARENT.eq(0),
                            GoogleTask.TASK.eq(Task.ID),
                            GoogleTask.DELETED.eq(0))))
                .where(TaskCriteria.activeAndVisible())
                .toString();
        subtaskQuery
            .join(Join.inner(RECURSIVE, GoogleTask.PARENT.eq(RECURSIVE_TASK)))
            .join(
                Join.inner(
                    GoogleTask.TABLE,
                    Criterion.and(
                        GoogleTask.LIST.eq(list.getRemoteId()),
                        GoogleTask.PARENT.gt(0),
                        GoogleTask.TASK.eq(Task.ID),
                        GoogleTask.DELETED.eq(0))))
            .where(TaskCriteria.activeAndVisible());
      }

      String sortSelect = SortHelper.orderSelectForSortTypeRecursive(preferences.getSortMode());
      String withClause = "CREATE TEMPORARY TABLE `recursive_tasks` AS\n"
              + "WITH RECURSIVE recursive_tasks (task, indent, title, sortField) AS (\n"
              + " SELECT tasks._id, 0 AS sort_indent, UPPER(title) AS sort_title, "
              + sortSelect
              + " FROM tasks\n"
              + parentQuery
              + "\nUNION ALL SELECT tasks._id, recursive_tasks.indent+1 AS sort_indent, UPPER(tasks.title) AS sort_title, "
              + sortSelect
              + " FROM tasks\n"
              + subtaskQuery
              + "\nORDER BY sort_indent DESC, "
              + SortHelper.orderForSortTypeRecursive(preferences)
              + ") SELECT * FROM recursive_tasks";


      return newArrayList(
          "DROP TABLE IF EXISTS `temp`.`recursive_tasks`",
          SortHelper.adjustQueryForFlags(preferences, withClause),
          Query.select(fields.toArray(new Field[0]))
              .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(joinedQuery))
              .from(Task.TABLE)
              .toString());
    } else {
      fields.add(TAGS);

      // TODO: For now, we'll modify the query to join and include the things like tag data here.
      // Eventually, we might consider restructuring things so that this query is constructed
      // elsewhere.
      String joinedQuery =
          Join.left(Tag.TABLE.as(TAGS_METADATA_JOIN), tagsJoinCriterion).toString()
              + joins
              + filter.getSqlQuery();

      String query =
              SortHelper.adjustQueryForFlagsAndSort(preferences, joinedQuery, preferences.getSortMode());

      String groupedQuery =
              query.contains("ORDER BY")
                      ? query.replace("ORDER BY", "GROUP BY " + Task.ID + " ORDER BY")
                      : query + " GROUP BY " + Task.ID;

      return newArrayList(
          Query.select(fields.toArray(new Field[0]))
              .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(groupedQuery))
              .from(Task.TABLE)
              .toString());
    }
  }

  public void searchByFilter(Filter filter) {
    this.filter = filter;
    invalidate();
  }

  public void invalidate() {
    assertMainThread();

    List<String> queries = getQuery(preferences, filter);
    disposable.add(
        Single.fromCallable(() -> taskDao.fetchTasks(queries))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(tasks::setValue, Timber::e));
  }

  @Override
  protected void onCleared() {
    disposable.dispose();
  }

  public List<TaskContainer> getValue() {
    List<TaskContainer> value = tasks.getValue();
    return value != null ? value : Collections.emptyList();
  }
}
