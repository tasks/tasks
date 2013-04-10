/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.TitleParser;


/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskService {

    public static final String TRANS_QUICK_ADD_MARKUP = "markup"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_CHANGED = "repeat_changed"; //$NON-NLS-1$

    public static final String TRANS_TAGS = "tags"; //$NON-NLS-1$

    public static final String TRANS_ASSIGNED = "task-assigned"; //$NON-NLS-1$

    public static final String TRANS_EDIT_SAVE = "task-edit-save"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_COMPLETE = "repeat-complete"; //$NON-NLS-1$

    private static final int TOTAL_TASKS_FOR_ACTIVATION = 3;
    private static final int COMPLETED_TASKS_FOR_ACTIVATION = 1;
    private static final String PREF_USER_ACTVATED = "user-activated"; //$NON-NLS-1$

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private UserActivityDao userActivityDao;

    public TaskService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<Task> query(Query query) {
        return taskDao.query(query);
    }

    /**
     *
     * @param properties
     * @param id id
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(long id, Property<?>... properties) {
        return taskDao.fetch(id, properties);
    }

    /**
     *
     * @param uuid
     * @param properties
     * @return item, or null if it doesn't exist
     */
    public Task fetchByUUID(String uuid, Property<?>... properties) {
        TodorooCursor<Task> task = query(Query.select(properties).where(Task.UUID.eq(uuid)));
        try {
            if (task.getCount() > 0) {
                task.moveToFirst();
                return new Task(task);
            }
            return null;
        } finally {
            task.close();
        }
    }

    /**
     * Mark the given task as completed and save it.
     *
     * @param item
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setValue(Task.COMPLETION_DATE, DateUtilities.now());

            long reminderLast = item.getValue(Task.REMINDER_LAST);
            String socialReminder = item.getValue(Task.SOCIAL_REMINDER);
            if (reminderLast > 0) {
                long diff = DateUtilities.now() - reminderLast;
                if (diff > 0 && diff < DateUtilities.ONE_DAY) {
                    // within one day of last reminder
                    StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_ONE_DAY, "social", socialReminder); //$NON-NLS-1$
                }
                if (diff > 0 && diff < DateUtilities.ONE_WEEK) {
                    // within one week of last reminder
                    StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_ONE_WEEK, "social", socialReminder); //$NON-NLS-1$
                }
            }
            StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_V2);
        } else {
            item.setValue(Task.COMPLETION_DATE, 0L);
        }

        taskDao.save(item);
    }

    /**
     * Create or save the given action item
     *
     * @param item
     * @param skipHooks
     *            Whether pre and post hooks should run. This should be set
     *            to true if tasks are created as part of synchronization
     */
    public boolean save(Task item) {
        return taskDao.save(item);
    }

    /**
     * Clone the given task and all its metadata
     *
     * @param the old task
     * @return the new task
     */
    public Task clone(Task task) {
        Task newTask = fetchById(task.getId(), Task.PROPERTIES);
        if(newTask == null)
            return new Task();
        newTask.clearValue(Task.ID);
        newTask.clearValue(Task.UUID);
        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            if(cursor.getCount() > 0) {
                Metadata metadata = new Metadata();
                newTask.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
                taskDao.save(newTask);
                long newId = newTask.getId();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    metadata.readFromCursor(cursor);

                    if(!metadata.containsNonNullValue(Metadata.KEY))
                        continue;

                    if(GtasksMetadata.METADATA_KEY.equals(metadata.getValue(Metadata.KEY)))
                        metadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
                    if(OpencrxCoreUtils.OPENCRX_ACTIVITY_METADATA_KEY.equals(metadata.getValue(Metadata.KEY)))
                        metadata.setValue(OpencrxCoreUtils.ACTIVITY_ID, 0L);

                    metadata.setValue(Metadata.TASK, newId);
                    metadata.clearValue(Metadata.ID);
                    metadataDao.createNew(metadata);
                }
            }
        } finally {
            cursor.close();
        }
        return newTask;
    }

    public Task cloneReusableTask(Task task, String tagName, String tagUuid) {
        Task newTask = fetchById(task.getId(), Task.PROPERTIES);
        if (newTask == null)
            return new Task();
        newTask.clearValue(Task.ID);
        newTask.clearValue(Task.UUID);
        newTask.clearValue(Task.USER);
        newTask.clearValue(Task.USER_ID);
        newTask.clearValue(Task.IS_READONLY);
        newTask.clearValue(Task.IS_PUBLIC);

        taskDao.save(newTask);

        if (!RemoteModel.isUuidEmpty(tagUuid)) {
            TagService.getInstance().createLink(newTask, tagName, tagUuid);
        }
        return newTask;
    }

    /**
     * Delete the given task. Instead of deleting from the database, we set
     * the deleted flag.
     *
     * @param model
     */
    public void delete(Task item) {
        if(!item.isSaved())
            return;
        else if(item.containsValue(Task.TITLE) && item.getValue(Task.TITLE).length() == 0) {
            taskDao.delete(item.getId());
            taskOutstandingDao.deleteWhere(TaskOutstanding.ENTITY_ID_PROPERTY.eq(item.getId()));
            item.setId(Task.NO_ID);
        } else {
            long id = item.getId();
            item.clear();
            item.setId(id);
            GCalHelper.deleteTaskEvent(item);
            item.setValue(Task.DELETION_DATE, DateUtilities.now());
            taskDao.save(item);
        }
    }

    /**
     * Permanently delete the given task.
     *
     * @param model
     */
    public void purge(long taskId) {
        taskDao.delete(taskId);
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Task> cursor = taskDao.query(
                Query.select(Task.ID).where(TaskCriteria.hasNoTitle()));
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                taskDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Fetch tasks for the given filter
     * @param properties
     * @param constraint text constraint, or null
     * @param filter
     * @return
     */
    @SuppressWarnings("nls")
    public TodorooCursor<Task> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null)
            whereConstraint = Functions.upper(Task.TITLE).like("%" +
                    constraint.toString().toUpperCase() + "%");

        if(queryTemplate == null) {
            if(whereConstraint == null)
                return taskDao.query(Query.selectDistinct(properties));
            else
                return taskDao.query(Query.selectDistinct(properties).where(whereConstraint));
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE"))
                sql = queryTemplate + " WHERE " + whereConstraint;
            else
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
        } else
            sql = queryTemplate;

        sql = PermaSql.replacePlaceholders(sql);

        return taskDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    public boolean getUserActivationStatus() {
        if (Preferences.getBoolean(PREF_USER_ACTVATED, false))
            return true;

        TodorooCursor<Task> all = query(Query.select(Task.ID).limit(TOTAL_TASKS_FOR_ACTIVATION));
        try {
            if (all.getCount() < TOTAL_TASKS_FOR_ACTIVATION)
                return false;

            TodorooCursor<Task> completed = query(Query.select(Task.ID).where(TaskCriteria.completed()).limit(COMPLETED_TASKS_FOR_ACTIVATION));
            try {
                if (completed.getCount() < COMPLETED_TASKS_FOR_ACTIVATION)
                    return false;
            } finally {
                completed.close();
            }
        } finally {
            all.close();
        }

        Preferences.setBoolean(PREF_USER_ACTVATED, true);
        return true;
    }

    /**
     * @param query
     * @return how many tasks are matched by this query
     */
    public int count(Query query) {
        return taskDao.count(query);
    }

    /**
     * Clear details cache. Useful if user performs some operation that
     * affects details
     * @param criterion
     *
     * @return # of affected rows
     */
    public int clearDetails(Criterion criterion) {
        Task template = new Task();
        template.setValue(Task.DETAILS_DATE, 0L);
        return taskDao.update(criterion, template);
    }

    /**
     * Update database based on selection and values
     * @param selection
     * @param selectionArgs
     * @param setValues
     * @return
     */
    public int updateBySelection(String selection, String[] selectionArgs,
            Task taskValues) {
        TodorooCursor<Task> cursor = taskDao.rawQuery(selection, selectionArgs, Task.ID);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                taskValues.setValue(Task.ID, cursor.get(Task.ID));
                taskDao.save(taskValues);
            }
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Update all matching a clause to have the values set on template object.
     * <p>
     * Example (updates "joe" => "bob" in metadata value1):
     * {code}
     * Metadata item = new Metadata();
     * item.setValue(Metadata.VALUE1, "bob");
     * update(item, Metadata.VALUE1.eq("joe"));
     * {code}
     * @param where sql criteria
     * @param template set fields on this object in order to set them in the db.
     * @return # of updated items
     */
    public int update(Criterion where, Task template) {
        return taskDao.update(where, template);
    }

    /**
     * Count tasks overall
     * @param filter
     * @return
     */
    public int countTasks() {
        TodorooCursor<Task> cursor = query(Query.select(Task.ID));
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /** count tasks in a given filter */
    public int countTasks(Filter filter) {
        String queryTemplate = PermaSql.replacePlaceholders(filter.getSqlQuery());
        TodorooCursor<Task> cursor = query(Query.select(Task.ID).withQueryTemplate(
                queryTemplate));
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete all tasks matching a given criterion
     * @param all
     */
    public int deleteWhere(Criterion criteria) {
        return taskDao.deleteWhere(criteria);
    }

    /**
     * Save task, parsing quick-add mark-up:
     * <ul>
     * <li>#tag - add the tag "tag"
     * <li>@context - add the tag "@context"
     * <li>!4 - set priority to !!!!
     */
    private void quickAdd(Task task, List<String> tags) {
        save(task);
        for(String tag : tags) {
            TagService.getInstance().createLink(task, tag);
        }
    }

    /**
     * Parse quick add markup for the given task
     * @param task
     * @param tags an empty array to apply tags to
     * @return
     */
    public static boolean parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        return TitleParser.parse(task, tags);
    }

    /**
     * Create an uncompleted copy of this task and edit it
     * @param itemId
     * @return cloned item id
     */
    public long duplicateTask(long itemId) {
        Task original = new Task();
        original.setId(itemId);
        Task clone = clone(original);
        String userId = clone.getValue(Task.USER_ID);
        if (!Task.USER_ID_SELF.equals(userId) && !ActFmPreferenceService.userId().equals(userId))
            clone.putTransitory(TRANS_ASSIGNED, true);
        clone.setValue(Task.CREATION_DATE, DateUtilities.now());
        clone.setValue(Task.COMPLETION_DATE, 0L);
        clone.setValue(Task.DELETION_DATE, 0L);
        clone.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
        GCalHelper.createTaskEventIfEnabled(clone);

        save(clone);
        return clone.getId();
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     *
     * @param values
     * @param title
     * @param taskService
     * @param metadataService
     * @return
     */
    public static Task createWithValues(ContentValues values, String title) {
        Task task = new Task();
        return createWithValues(task, values, title);
    }

    /**
     * Create task from the given content values, saving it.
     *
     * @param task base task to start with
     * @param values
     * @param title
     * @param taskService
     * @param metadataService
     * @return
     */
    public static Task createWithValues(Task task, ContentValues values, String title) {
        if (title != null)
            task.setValue(Task.TITLE, title);

        ArrayList<String> tags = new ArrayList<String>();
        boolean quickAddMarkup = false;
        try {
            quickAddMarkup = parseQuickAddMarkup(task, tags);
        } catch (Throwable e) {
            PluginServices.getExceptionService().reportError("parse-quick-add", e); //$NON-NLS-1$
        }

        ContentValues forMetadata = null;
        if (values != null && values.size() > 0) {
            ContentValues forTask = new ContentValues();
            forMetadata = new ContentValues();
            outer: for (Entry<String, Object> item : values.valueSet()) {
                String key = item.getKey();
                Object value = item.getValue();
                if (value instanceof String)
                    value = PermaSql.replacePlaceholders((String) value);

                for (Property<?> property : Metadata.PROPERTIES)
                    if (property.name.equals(key)) {
                        AndroidUtilities.putInto(forMetadata, key, value, true);
                        continue outer;
                    }

                AndroidUtilities.putInto(forTask, key, value, true);
            }
            task.mergeWithoutReplacement(forTask);
        }

        if (!Task.USER_ID_SELF.equals(task.getValue(Task.USER_ID)))
            task.putTransitory(TRANS_ASSIGNED, true);

        PluginServices.getTaskService().quickAdd(task, tags);
        if (quickAddMarkup)
            task.putTransitory(TRANS_QUICK_ADD_MARKUP, true);

        if (forMetadata != null && forMetadata.size() > 0) {
            Metadata metadata = new Metadata();
            metadata.setValue(Metadata.TASK, task.getId());
            metadata.mergeWith(forMetadata);
            if (TaskToTagMetadata.KEY.equals(metadata.getValue(Metadata.KEY))) {
                if (metadata.containsNonNullValue(TaskToTagMetadata.TAG_UUID) && !RemoteModel.NO_UUID.equals(metadata.getValue(TaskToTagMetadata.TAG_UUID))) {
                    // This is more efficient
                    TagService.getInstance().createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME), metadata.getValue(TaskToTagMetadata.TAG_UUID));
                } else {
                    // This is necessary for backwards compatibility
                    TagService.getInstance().createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME));
                }
            } else {
                PluginServices.getMetadataService().save(metadata);
            }
        }

        return task;
    }

    public TodorooCursor<UserActivity> getActivityAndHistoryForTask(Task task) {
        Query taskQuery = queryForTask(task, UpdateAdapter.USER_TABLE_ALIAS, UpdateAdapter.USER_ACTIVITY_PROPERTIES, UpdateAdapter.USER_PROPERTIES);

        Query historyQuery = Query.select(AndroidUtilities.addToArray(Property.class, UpdateAdapter.HISTORY_PROPERTIES, UpdateAdapter.USER_PROPERTIES)).from(History.TABLE)
                .where(Criterion.and(History.TABLE_ID.eq(NameMaps.TABLE_ID_TASKS), History.TARGET_ID.eq(task.getUuid())))
                .from(History.TABLE)
                .join(Join.left(User.TABLE.as(UpdateAdapter.USER_TABLE_ALIAS), History.USER_UUID.eq(Field.field(UpdateAdapter.USER_TABLE_ALIAS + "." + User.UUID.name)))); //$NON-NLS-1$;

        Query resultQuery = taskQuery.union(historyQuery).orderBy(Order.desc("1")); //$NON-NLS-1$

        return userActivityDao.query(resultQuery);
    }

    private static Query queryForTask(Task task, String userTableAlias, Property<?>[] activityProperties, Property<?>[] userProperties) {
        Query result = Query.select(AndroidUtilities.addToArray(Property.class, activityProperties, userProperties))
                .where(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT), UserActivity.TARGET_ID.eq(task.getUuid()), UserActivity.DELETED_AT.eq(0)));
        if (!TextUtils.isEmpty(userTableAlias))
            result = result.join(Join.left(User.TABLE.as(userTableAlias), UserActivity.USER_UUID.eq(Field.field(userTableAlias + "." + User.UUID.name)))); //$NON-NLS-1$
        return result;
    }

}
