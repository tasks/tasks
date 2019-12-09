/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.dao;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.sql.SqlConstants.COUNT;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static org.tasks.db.DbUtils.batch;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.data.TaskContainer;
import org.tasks.jobs.WorkManager;
import timber.log.Timber;

@Dao
public abstract class TaskDao {

  public static final String TRANS_SUPPRESS_REFRESH = "suppress-refresh";

  private final Database database;

  private WorkManager workManager;

  public TaskDao(Database database) {
    this.database = database;
  }

  public void initialize(WorkManager workManager) {
    this.workManager = workManager;
  }

  public List<Task> needsRefresh() {
    return needsRefresh(now());
  }

  @Query(
      "SELECT * FROM tasks WHERE completed = 0 AND deleted = 0 AND (hideUntil > :now OR dueDate > :now)")
  abstract List<Task> needsRefresh(long now);

  @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
  public abstract Task fetch(long id);

  @Query("SELECT * FROM tasks WHERE _id IN (:taskIds)")
  public abstract List<Task> fetch(List<Long> taskIds);

  @Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0 AND deleted = 0")
  public abstract int activeTimers();

  @Query("SELECT tasks.* FROM tasks INNER JOIN notification ON tasks._id = notification.task")
  public abstract List<Task> activeNotifications();

  @Query("SELECT * FROM tasks WHERE remoteId = :remoteId")
  public abstract Task fetch(String remoteId);

  @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0")
  abstract List<Task> getActiveTasks();

  @Query("SELECT * FROM tasks WHERE hideUntil < (strftime('%s','now')*1000)")
  abstract List<Task> getVisibleTasks();

  @Query(
      "SELECT * FROM tasks WHERE remoteId IN (:remoteIds) "
          + "AND recurrence IS NOT NULL AND LENGTH(recurrence) > 0")
  public abstract List<Task> getRecurringTasks(List<String> remoteIds);

  @Query("UPDATE tasks SET completed = :completionDate " + "WHERE remoteId = :remoteId")
  public abstract void setCompletionDate(String remoteId, long completionDate);

  @Query("UPDATE tasks SET snoozeTime = :millis WHERE _id in (:taskIds)")
  public abstract void snooze(List<Long> taskIds, long millis);

  @Query(
      "SELECT tasks.* FROM tasks "
          + "LEFT JOIN google_tasks ON tasks._id = google_tasks.gt_task "
          + "WHERE gt_list_id IN (SELECT gtl_remote_id FROM google_task_lists WHERE gtl_account = :account)"
          + "AND (tasks.modified > google_tasks.gt_last_sync OR google_tasks.gt_remote_id = '') "
          + "ORDER BY CASE WHEN gt_parent = 0 THEN 0 ELSE 1 END, gt_order ASC")
  public abstract List<Task> getGoogleTasksToPush(String account);

  @Query(
      "SELECT tasks.* FROM tasks "
          + "LEFT JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task "
          + "WHERE caldav_tasks.cd_calendar = :calendar "
          + "AND tasks.modified > caldav_tasks.cd_last_sync")
  public abstract List<Task> getCaldavTasksToPush(String calendar);

  @Query(
      "SELECT * FROM TASKS "
          + "WHERE completed = 0 AND deleted = 0 AND (notificationFlags > 0 OR notifications > 0)")
  public abstract List<Task> getTasksWithReminders();

  // --- SQL clause generators

  @Query("SELECT * FROM tasks")
  public abstract List<Task> getAll();

  @Query("SELECT calendarUri FROM tasks " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
  public abstract List<String> getAllCalendarEvents();

  @Query("UPDATE tasks SET calendarUri = '' " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
  public abstract int clearAllCalendarEvents();

  @Query(
      "SELECT calendarUri FROM tasks "
          + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
  public abstract List<String> getCompletedCalendarEvents();

  @Query(
      "UPDATE tasks SET calendarUri = '' "
          + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
  public abstract int clearCompletedCalendarEvents();

  @Transaction
  public List<TaskContainer> fetchTasks(QueryCallback callback) {
    long start = BuildConfig.DEBUG ? now() : 0;
    boolean includeGoogleSubtasks = atLeastLollipop() && hasGoogleTaskSubtasks();
    boolean includeCaldavSubtasks = atLeastLollipop() && hasCaldavSubtasks();
    List<String> queries = callback.getQueries(includeGoogleSubtasks, includeCaldavSubtasks);
    SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
    int last = queries.size() - 1;
    for (int i = 0 ; i < last ; i++) {
      db.execSQL(queries.get(i));
    }
    List<TaskContainer> result = fetchTasks(new SimpleSQLiteQuery(queries.get(last)));
    Timber.v("%sms: %s", now() - start, Joiner.on(";").join(queries));
    return result;
  }

  @RawQuery
  abstract List<TaskContainer> fetchTasks(SimpleSQLiteQuery query);

  @RawQuery
  abstract int count(SimpleSQLiteQuery query);

  @Query("SELECT EXISTS(SELECT 1 FROM caldav_tasks WHERE cd_parent > 0 AND cd_deleted = 0)")
  abstract boolean hasCaldavSubtasks();

  @Query("SELECT EXISTS(SELECT 1 FROM google_tasks WHERE gt_parent > 0 AND gt_deleted = 0)")
  abstract boolean hasGoogleTaskSubtasks();

  @Query("UPDATE tasks SET modified = datetime('now', 'localtime') WHERE _id in (:ids)")
  public abstract void touch(List<Long> ids);

  @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id = :id")
  public abstract void setCollapsed(long id, boolean collapsed);

  @Transaction
  public void setCollapsed(List<TaskContainer> tasks, boolean collapsed) {
    batch(
        transform(filter(tasks, TaskContainer::hasChildren), TaskContainer::getId),
        l -> collapse(l, collapsed));
  }

  @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id IN (:ids)")
  abstract void collapse(List<Long> ids, boolean collapsed);

  /**
   * Saves the given task to the database.getDatabase(). Task must already exist. Returns true on
   * success.
   */
  public void save(Task task) {
    save(task, fetch(task.getId()));
  }

  // --- save

  // TODO: get rid of this super-hack
  public void save(Task task, Task original) {
    if (saveExisting(task, original)) {
      workManager.afterSave(task, original);
    }
  }

  @Insert
  abstract long insert(Task task);

  @Update
  abstract int update(Task task);

  public void createNew(Task task) {
    task.id = null;
    if (task.created == 0) {
      task.created = now();
    }
    if (Task.isUuidEmpty(task.remoteId)) {
      task.remoteId = UUIDHelper.newUUID();
    }
    long insert = insert(task);
    task.setId(insert);
  }

  private boolean saveExisting(Task item, Task original) {
    if (!item.insignificantChange(original)) {
      item.setModificationDate(now());
    }
    return update(item) == 1;
  }

  @Query(
      "SELECT * FROM tasks "
          + "WHERE completed = 0 AND deleted = 0 AND hideUntil < (strftime('%s','now')*1000) "
          + "ORDER BY (CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE ((CASE WHEN (dueDate / 1000) % 60 > 0 THEN dueDate ELSE (dueDate + 43140000) END)) END) + 172800000 * importance ASC "
          + "LIMIT 100")
  public abstract List<Task> getAstrid2TaskProviderTasks();

  public int count(Filter filter) {
    SimpleSQLiteQuery query = getQuery(filter.sqlQuery, COUNT);
    long start = BuildConfig.DEBUG ? now() : 0;
    int count = count(query);
    Timber.v("%sms: %s", now() - start, query.getSql());
    return count;
  }

  public List<Task> fetchFiltered(Filter filter) {
    return fetchFiltered(filter.getSqlQuery());
  }

  public List<Task> fetchFiltered(String queryTemplate) {
    SimpleSQLiteQuery query = getQuery(queryTemplate, Task.FIELDS);
    long start = BuildConfig.DEBUG ? now() : 0;
    List<TaskContainer> tasks = fetchTasks(query);
    Timber.v("%sms: %s", now() - start, query.getSql());
    return transform(tasks, TaskContainer::getTask);
  }

  private static SimpleSQLiteQuery getQuery(String queryTemplate, Field... fields) {
    return new SimpleSQLiteQuery(
        com.todoroo.andlib.sql.Query.select(fields)
            .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(queryTemplate))
            .from(Task.TABLE)
            .toString());
  }

  /** Generates SQL clauses */
  public static class TaskCriteria {

    /** @return tasks that were not deleted */
    public static Criterion notDeleted() {
      return Task.DELETION_DATE.eq(0);
    }

    public static Criterion notCompleted() {
      return Task.COMPLETION_DATE.eq(0);
    }

    public static Criterion includeCompleted() {
      return Task.COMPLETION_DATE.gte(0);
    }

    /** @return tasks that have not yet been completed or deleted */
    public static Criterion activeAndVisible() {
      return Criterion.and(
          Task.COMPLETION_DATE.eq(0),
          Task.DELETION_DATE.eq(0),
          Task.HIDE_UNTIL.lt(Functions.now()));
    }

    /** @return tasks that have not yet been completed or deleted */
    public static Criterion isActive() {
      return Criterion.and(Task.COMPLETION_DATE.eq(0), Task.DELETION_DATE.eq(0));
    }

    /** @return tasks that are not hidden at current time */
    public static Criterion isVisible() {
      return Task.HIDE_UNTIL.lt(Functions.now());
    }

    public static Criterion includeHidden() {
      return Task.HIDE_UNTIL.gte(0);
    }
  }

  public interface QueryCallback {
    List<String> getQueries(boolean includeGoogleTaskSubtasks, boolean includeCaldavSubtasks);
  }
}
