package org.tasks.data;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.sql.Field.field;
import static com.todoroo.astrid.activity.TaskListFragment.CALDAV_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.GTASK_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.TAGS_METADATA_JOIN;

import com.google.common.collect.ImmutableList;
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
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.List;
import org.tasks.preferences.Preferences;

public class TaskListQuery {

  private static final Criterion JOIN_GTASK =
      Criterion.and(
          Task.ID.eq(field(GTASK_METADATA_JOIN + ".gt_task")),
          field(GTASK_METADATA_JOIN + ".gt_deleted").eq(0));
  private static final Criterion JOIN_CALDAV =
      Criterion.and(
          Task.ID.eq(field(CALDAV_METADATA_JOIN + ".cd_task")),
          field(CALDAV_METADATA_JOIN + ".cd_deleted").eq(0));
  private static final Criterion JOIN_TAGS = Task.ID.eq(field(TAGS_METADATA_JOIN + ".task"));
  private static final String JOINS =
      Join.left(GoogleTask.TABLE.as(GTASK_METADATA_JOIN), JOIN_GTASK).toString()
          + Join.left(CaldavTask.TABLE.as(CALDAV_METADATA_JOIN), JOIN_CALDAV)
          + Join.left(Geofence.TABLE, Geofence.TASK.eq(Task.ID))
          + Join.left(Place.TABLE, Place.UID.eq(Geofence.PLACE));

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
  private static final Field TAG_QUERY =
      field(
              "("
                  + Query.select(field("group_concat(distinct(tag_uid))"))
                      .from(Tag.TABLE)
                      .where(Task.ID.eq(Tag.TASK))
                      .toString()
                  + " GROUP BY "
                  + Tag.TASK
                  + ")")
          .as("tags");
  private static final StringProperty TAGS =
      new StringProperty(null, "group_concat(distinct(" + TAGS_METADATA_JOIN + ".tag_uid)" + ")")
          .as("tags");
  private static final List<Field> FIELDS = ImmutableList.of(TASKS, GTASK, CALDAV, GEOFENCE, PLACE);

  public static List<String> getQuery(
      Preferences preferences,
      com.todoroo.astrid.api.Filter filter,
      boolean includeGoogleTaskSubtasks,
      boolean includeCaldavSubtasks) {
    if (filter.supportSubtasks()
        && (includeGoogleTaskSubtasks || includeCaldavSubtasks)
        && preferences.showSubtasks()
        && !(preferences.isManualSort() && filter.supportsManualSort())) {
      return getRecursiveQuery(
          filter, preferences, includeGoogleTaskSubtasks, includeCaldavSubtasks);
    } else {
      return getNonRecursiveQuery(filter, preferences);
    }
  }

  private static List<String> getRecursiveQuery(
      com.todoroo.astrid.api.Filter filter,
      Preferences preferences,
      boolean includeGoogleTaskSubtasks,
      boolean includeCaldavSubtasks) {
    List<Field> fields = newArrayList(FIELDS);
    fields.add(TAG_QUERY);
    fields.add(INDENT);
    fields.add(CHILDREN);

    String joinedQuery =
        Join.inner(RECURSIVE, Task.ID.eq(RECURSIVE_TASK))
            + " LEFT JOIN (SELECT parent, count(distinct recursive_tasks.task) AS children FROM recursive_tasks GROUP BY parent) AS recursive_children ON recursive_children.parent = tasks._id "
            + JOINS;
    String where = " WHERE recursive_tasks.hidden = 0";
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
                          CaldavTask.TASK.eq(Task.ID),
                          CaldavTask.DELETED.eq(0))))
              .where(Criterion.and(TaskCriteria.activeAndVisible(), Task.PARENT.eq(0)))
              .toString();
      subtaskQuery
          .join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)))
          .where(TaskCriteria.activeAndVisible());
    } else if (filter instanceof GtasksFilter) {
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
                  Criterion.and(GoogleTask.TASK.eq(Task.ID), GoogleTask.DELETED.eq(0))))
          .where(TaskCriteria.activeAndVisible());
    } else {
      parentQuery = PermaSql.replacePlaceholdersForQuery(filter.getSqlQuery());
      if (includeGoogleTaskSubtasks && includeCaldavSubtasks) {
        addGoogleAndCaldavSubtasks(subtaskQuery);
      } else if (includeGoogleTaskSubtasks) {
        addGoogleSubtasks(subtaskQuery);
      } else {
        addCaldavSubtasks(subtaskQuery);
      }
      subtaskQuery.where(TaskCriteria.activeAndVisible());
      joinedQuery +=
          " LEFT JOIN (SELECT task, max(indent) AS max_indent FROM recursive_tasks GROUP BY task) AS recursive_indents ON recursive_indents.task = tasks._id ";
      where += " AND indent = max_indent ";
    }
    joinedQuery += where;

    String sortSelect = SortHelper.orderSelectForSortTypeRecursive(preferences.getSortMode());
    String withClause =
        "CREATE TEMPORARY TABLE `recursive_tasks` AS\n"
            + "WITH RECURSIVE recursive_tasks (task, parent, collapsed, hidden, indent, title, sortField) AS (\n"
            + " SELECT tasks._id, 0 as parent, tasks.collapsed as collapsed, 0 as hidden, 0 AS sort_indent, UPPER(tasks.title) AS sort_title, "
            + sortSelect
            + " FROM tasks\n"
            + parentQuery
            + "\nUNION ALL SELECT tasks._id, recursive_tasks.task as parent, tasks.collapsed as collapsed, CASE WHEN recursive_tasks.collapsed > 0 OR recursive_tasks.hidden > 0 THEN 1 ELSE 0 END as hidden, recursive_tasks.indent+1 AS sort_indent, UPPER(tasks.title) AS sort_title, "
            + sortSelect
            + " FROM tasks\n"
            + subtaskQuery
            + "\nORDER BY sort_indent DESC, "
            + SortHelper.orderForSortTypeRecursive(preferences)
            + ") SELECT * FROM recursive_tasks";

    return newArrayList(
        "DROP TABLE IF EXISTS `temp`.`recursive_tasks`",
        SortHelper.adjustQueryForFlags(preferences, withClause),
        "CREATE INDEX `r_tasks` ON `recursive_tasks` (`task`)",
        "CREATE INDEX `r_parents` ON `recursive_tasks` (`parent`)",
        Query.select(fields.toArray(new Field[0]))
            .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(joinedQuery))
            .from(Task.TABLE)
            .toString());
  }

  private static List<String> getNonRecursiveQuery(Filter filter, Preferences preferences) {
    List<Field> fields = newArrayList(FIELDS);
    fields.add(TAGS);

    if (filter instanceof GtasksFilter && preferences.isManualSort()) {
      fields.add(INDENT);
      fields.add(CHILDREN);
      fields.add(SIBLINGS);
      fields.add(PRIMARY_SORT);
      fields.add(SECONDARY_SORT);
    }
    // TODO: For now, we'll modify the query to join and include the things like tag data here.
    // Eventually, we might consider restructuring things so that this query is constructed
    // elsewhere.

    String joinedQuery =
        Join.left(Tag.TABLE.as(TAGS_METADATA_JOIN), JOIN_TAGS).toString()
            + JOINS
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

  private static void addGoogleSubtasks(QueryTemplate subtaskQuery) {
    subtaskQuery
        .join(Join.inner(RECURSIVE, GoogleTask.PARENT.eq(RECURSIVE_TASK)))
        .join(
            Join.inner(
                GoogleTask.TABLE,
                Criterion.and(GoogleTask.TASK.eq(Task.ID), GoogleTask.DELETED.eq(0))));
  }

  private static void addCaldavSubtasks(QueryTemplate subtaskQuery) {
    subtaskQuery.join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)));
  }

  private static void addGoogleAndCaldavSubtasks(QueryTemplate subtaskQuery) {
    subtaskQuery
        .join(
            Join.inner(
                RECURSIVE,
                Criterion.or(GoogleTask.PARENT.eq(RECURSIVE_TASK), Task.PARENT.eq(RECURSIVE_TASK))))
        .join(
            Join.left(
                GoogleTask.TABLE,
                Criterion.and(GoogleTask.TASK.eq(Task.ID), GoogleTask.DELETED.eq(0))))
        .join(
            Join.left(
                CaldavTask.TABLE,
                Criterion.and(CaldavTask.TASK.eq(Task.ID), CaldavTask.DELETED.eq(0))));
  }
}
