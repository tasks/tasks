/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.content.Intent;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.TitleParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.filters.FilterCounter;
import org.tasks.scheduling.RefreshScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    public static final String TRANS_QUICK_ADD_MARKUP = "markup"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_CHANGED = "repeat_changed"; //$NON-NLS-1$

    public static final String TRANS_TAGS = "tags"; //$NON-NLS-1$

    public static final String TRANS_EDIT_SAVE = "task-edit-save"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_COMPLETE = "repeat-complete"; //$NON-NLS-1$

    private final TaskDao taskDao;
    private final UserActivityDao userActivityDao;
    private final Broadcaster broadcaster;
    private final FilterCounter filterCounter;
    private final RefreshScheduler refreshScheduler;
    private final TagService tagService;

    @Inject
    public TaskService(TaskDao taskDao, UserActivityDao userActivityDao,
                       Broadcaster broadcaster, FilterCounter filterCounter,
                       RefreshScheduler refreshScheduler, TagService tagService) {
        this.taskDao = taskDao;
        this.userActivityDao = userActivityDao;
        this.broadcaster = broadcaster;
        this.filterCounter = filterCounter;
        this.refreshScheduler = refreshScheduler;
        this.tagService = tagService;
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
            tagService.createLink(task, tag);
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
    public static boolean parseQuickAddMarkup(TagService tagService, Task task, ArrayList<String> tags) {
        return TitleParser.parse(tagService, task, tags);
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public static Task createWithValues(TaskService taskService, MetadataService metadataService, TagService tagService, ContentValues values, String title) {
        Task task = new Task();
        return createWithValues(taskService, metadataService, tagService, task, values, title);
    }

    /**
     * Create task from the given content values, saving it.
     * @param task base task to start with
     */
    public static Task createWithValues(TaskService taskService, MetadataService metadataService, TagService tagService, Task task, ContentValues values, String title) {
        if (title != null) {
            task.setTitle(title);
        }

        ArrayList<String> tags = new ArrayList<>();
        boolean quickAddMarkup = false;
        try {
            quickAddMarkup = parseQuickAddMarkup(tagService, task, tags);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
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

        taskService.quickAdd(task, tags);
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
                    tagService.createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME), metadata.getValue(TaskToTagMetadata.TAG_UUID));
                } else {
                    // This is necessary for backwards compatibility
                    tagService.createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME));
                }
            } else {
                metadataService.save(metadata);
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
