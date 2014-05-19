/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.Broadcaster;
import org.tasks.filters.FilterCounter;
import org.tasks.scheduling.RefreshScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;


/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskService {

    private static final String TAG = "TaskService";

    public static final String TRANS_QUICK_ADD_MARKUP = "markup"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_CHANGED = "repeat_changed"; //$NON-NLS-1$

    public static final String TRANS_TAGS = "tags"; //$NON-NLS-1$

    public static final String TRANS_ASSIGNED = "task-assigned"; //$NON-NLS-1$

    public static final String TRANS_EDIT_SAVE = "task-edit-save"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_COMPLETE = "repeat-complete"; //$NON-NLS-1$

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private UserActivityDao userActivityDao;

    @Autowired
    private Broadcaster broadcaster;

    @Autowired
    private FilterCounter filterCounter;

    @Autowired
    private RefreshScheduler refreshScheduler;

    public TaskService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Query underlying database
     */
    public TodorooCursor<Task> query(Query query) {
        return taskDao.query(query);
    }

    /**
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(long id, Property<?>... properties) {
        return taskDao.fetch(id, properties);
    }

    /**
     * Mark the given task as completed and save it.
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setCompletionDate(DateUtilities.now());
        } else {
            item.setCompletionDate(0L);
        }

        save(item);
    }

    /**
     * Create or save the given action item
     */
    public void save(Task item) {
        taskDao.save(item);
        broadcastFilterListUpdated();
        refreshScheduler.scheduleRefresh(item);
    }

    private void saveWithoutPublishingFilterUpdate(Task item) {
        taskDao.save(item);
    }

    /**
     * Clone the given task and all its metadata
     *
     * @return the new task
     */
    public Task clone(Task task) {
        Task newTask = fetchById(task.getId(), Task.PROPERTIES);
        if(newTask == null) {
            return new Task();
        }
        newTask.clearValue(Task.ID);
        newTask.clearValue(Task.UUID);
        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            if(cursor.getCount() > 0) {
                Metadata metadata = new Metadata();
                newTask.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
                save(newTask);
                long newId = newTask.getId();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    metadata.readFromCursor(cursor);

                    if(!metadata.containsNonNullValue(Metadata.KEY)) {
                        continue;
                    }

                    if(GtasksMetadata.METADATA_KEY.equals(metadata.getKey())) {
                        metadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
                    }

                    metadata.setTask(newId);
                    metadata.clearValue(Metadata.ID);
                    metadataDao.createNew(metadata);
                }
            }
        } finally {
            cursor.close();
        }
        return newTask;
    }

    /**
     * Delete the given task. Instead of deleting from the database, we set
     * the deleted flag.
     */
    public void delete(Task item) {
        if(!item.isSaved()) {
            return;
        }

        if(item.containsValue(Task.TITLE) && item.getTitle().length() == 0) {
            taskDao.delete(item.getId());
            item.setId(Task.NO_ID);
        } else {
            long id = item.getId();
            item.clear();
            item.setId(id);
            GCalHelper.deleteTaskEvent(item);
            item.setDeletionDate(DateUtilities.now());
            save(item);
        }
    }

    /**
     * Permanently delete the given task.
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
            if(cursor.getCount() == 0) {
                return;
            }

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
     * @param constraint text constraint, or null
     */
    public TodorooCursor<Task> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null) {
            whereConstraint = Functions.upper(Task.TITLE).like("%" +
                    constraint.toString().toUpperCase() + "%");
        }

        if(queryTemplate == null) {
            if(whereConstraint == null) {
                return taskDao.query(Query.selectDistinct(properties));
            } else {
                return taskDao.query(Query.selectDistinct(properties).where(whereConstraint));
            }
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE")) {
                sql = queryTemplate + " WHERE " + whereConstraint;
            } else {
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
            }
        } else {
            sql = queryTemplate;
        }

        sql = PermaSql.replacePlaceholders(sql);

        return taskDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    /**
     * @return how many tasks are matched by this query
     */
    public int count(Query query) {
        return taskDao.count(query);
    }

    /**
     * Clear details cache. Useful if user performs some operation that
     * affects details
     */
    public void clearDetails(Criterion criterion) {
        Task template = new Task();
        template.setDetailsDate(0L);
        taskDao.update(criterion, template);
    }

    /**
     * Update database based on selection and values
     */
    public int updateBySelection(String selection, String[] selectionArgs,
            Task taskValues) {
        TodorooCursor<Task> cursor = taskDao.rawQuery(selection, selectionArgs, Task.ID);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                taskValues.setID(cursor.get(Task.ID));
                save(taskValues);
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
     * item.setVALUE1("bob");
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
     */
    public int countTasks() {
        TodorooCursor<Task> cursor = query(Query.select(Task.ID));
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete all tasks matching a given criterion
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
        saveWithoutPublishingFilterUpdate(task);
        for(String tag : tags) {
            TagService.getInstance().createLink(task, tag);
        }
        broadcastFilterListUpdated();
    }

    private void broadcastFilterListUpdated() {
        filterCounter.refreshFilterCounts(new Runnable() {
            @Override
            public void run() {
                broadcaster.sendOrderedBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_FILTER_LIST_UPDATED));
            }
        });
    }

    /**
     * Parse quick add markup for the given task
     * @param tags an empty array to apply tags to
     */
    public static boolean parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        return TitleParser.parse(task, tags);
    }

    /**
     * Create an uncompleted copy of this task and edit it
     * @return cloned item id
     */
    public long duplicateTask(long itemId) {
        Task original = new Task();
        original.setId(itemId);
        Task clone = clone(original);
        String userId = clone.getUserID();
        if (!Task.USER_ID_SELF.equals(userId) && !ActFmPreferenceService.userId().equals(userId)) {
            clone.putTransitory(TRANS_ASSIGNED, true);
        }
        clone.setCreationDate(DateUtilities.now());
        clone.setCompletionDate(0L);
        clone.setDeletionDate(0L);
        clone.setCalendarUri(""); //$NON-NLS-1$
        GCalHelper.createTaskEventIfEnabled(clone);

        save(clone);
        return clone.getId();
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public static Task createWithValues(ContentValues values, String title) {
        Task task = new Task();
        return createWithValues(task, values, title);
    }

    /**
     * Create task from the given content values, saving it.
     * @param task base task to start with
     */
    public static Task createWithValues(Task task, ContentValues values, String title) {
        if (title != null) {
            task.setTitle(title);
        }

        ArrayList<String> tags = new ArrayList<>();
        boolean quickAddMarkup = false;
        try {
            quickAddMarkup = parseQuickAddMarkup(task, tags);
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }

        ContentValues forMetadata = null;
        if (values != null && values.size() > 0) {
            ContentValues forTask = new ContentValues();
            forMetadata = new ContentValues();
            outer: for (Entry<String, Object> item : values.valueSet()) {
                String key = item.getKey();
                Object value = item.getValue();
                if (value instanceof String) {
                    value = PermaSql.replacePlaceholders((String) value);
                }

                for (Property<?> property : Metadata.PROPERTIES) {
                    if (property.name.equals(key)) {
                        AndroidUtilities.putInto(forMetadata, key, value);
                        continue outer;
                    }
                }

                AndroidUtilities.putInto(forTask, key, value);
            }
            task.mergeWithoutReplacement(forTask);
        }

        if (!Task.USER_ID_SELF.equals(task.getUserID())) {
            task.putTransitory(TRANS_ASSIGNED, true);
        }

        PluginServices.getTaskService().quickAdd(task, tags);
        if (quickAddMarkup) {
            task.putTransitory(TRANS_QUICK_ADD_MARKUP, true);
        }

        if (forMetadata != null && forMetadata.size() > 0) {
            Metadata metadata = new Metadata();
            metadata.setTask(task.getId());
            metadata.mergeWith(forMetadata);
            if (TaskToTagMetadata.KEY.equals(metadata.getKey())) {
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

    public TodorooCursor<UserActivity> getActivityForTask(Task task) {
        Query taskQuery = queryForTask(task, UpdateAdapter.USER_ACTIVITY_PROPERTIES);

        Query resultQuery = taskQuery.orderBy(Order.desc("1")); //$NON-NLS-1$

        return userActivityDao.query(resultQuery);
    }

    private static Query queryForTask(Task task, Property<?>[] activityProperties) {
        return Query.select(AndroidUtilities.addToArray(Property.class, activityProperties))
                .where(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT), UserActivity.TARGET_ID.eq(task.getUuid()), UserActivity.DELETED_AT.eq(0)));
    }

}
