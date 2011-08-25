package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.weloveastrid.rmilk.data.MilkTaskFields;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.tags.TagService;

/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskService {

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

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
     * Mark the given task as completed and save it.
     *
     * @param item
     */
    public void setComplete(Task item, boolean completed) {
        if(completed)
            item.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        else
            item.setValue(Task.COMPLETION_DATE, 0L);
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
        newTask.clearValue(Task.REMOTE_ID);
        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            if(cursor.getCount() > 0) {
                Metadata metadata = new Metadata();
                long newId = newTask.getId();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    metadata.readFromCursor(cursor);

                    if(!metadata.containsNonNullValue(Metadata.KEY))
                        continue;

                    if(GtasksMetadata.METADATA_KEY.equals(metadata.getValue(Metadata.KEY)))
                        metadata.setValue(GtasksMetadata.ID, "0"); //$NON-NLS-1$
                    if(ProducteevTask.METADATA_KEY.equals(metadata.getValue(Metadata.KEY)))
                        metadata.setValue(ProducteevTask.ID, 0L);
                    if(MilkTaskFields.METADATA_KEY.equals(metadata.getValue(Metadata.KEY))) {
                        metadata.setValue(MilkTaskFields.TASK_ID, 0L);
                        metadata.setValue(MilkTaskFields.TASK_SERIES_ID, 0L);
                    }
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
            GCalHelper.deleteTaskEvent(item);
            taskDao.delete(item.getId());
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
                return taskDao.query(Query.select(properties));
            else
                return taskDao.query(Query.select(properties).where(whereConstraint));
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

    /**
     * @param query
     * @return how many tasks are matched by this query
     */
    public int count(Query query) {
        TodorooCursor<Task> cursor = taskDao.query(query);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Clear details cache. Useful if user performs some operation that
     * affects details
     * @param criterion
     *
     * @return # of affected rows
     */
    public int clearDetails(Criterion criterion) {
        ContentValues values = new ContentValues();
        values.put(Task.DETAILS_DATE.name, 0);
        return taskDao.updateMultiple(values, criterion);
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
        String queryTemplate = PermaSql.replacePlaceholders(filter.sqlQuery);
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
    public void quickAdd(Task task) {
        ArrayList<String> tags = new ArrayList<String>();
        parseQuickAddMarkup(task, tags);
        save(task);

        Metadata metadata = new Metadata();
        for(String tag : tags) {
            metadata.setValue(Metadata.KEY, TagService.KEY);
            metadata.setValue(Metadata.TASK, task.getId());
            metadata.setValue(TagService.TAG, tag);
            metadataDao.createNew(metadata);
        }
    }

    @SuppressWarnings("nls")
    public static void parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        String title = task.getValue(Task.TITLE);

        Pattern tagPattern = Pattern.compile("(\\s|^)#([^\\s]+)");
        Pattern contextPattern = Pattern.compile("(\\s|^)(@[^\\s]+)");
        Pattern importancePattern = Pattern.compile("(\\s|^)!(\\d)(\\s|$)");
        while(true) {
            Matcher m = tagPattern.matcher(title);
            if(m.find()) {
                tags.add(m.group(2));
            } else {
                m = contextPattern.matcher(title);
                if(m.find()) {
                    tags.add(m.group(2));
                } else {
                    m = importancePattern.matcher(title);
                    if(m.find()) {
                        int value = Integer.parseInt(m.group(2));
                        // not in producteev world: !1 to !4 => importance 3 to 0
                        int importance = Math.max(Task.IMPORTANCE_MOST, Task.IMPORTANCE_LEAST + 1 - value);
                        // in the producteev world, !1 to !4 => importance 4 to 1
                        if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
                            importance++;

                        task.setValue(Task.IMPORTANCE, importance);
                    } else
                        break;
                }
            }

            title = title.substring(0, m.start()) + title.substring(m.end());
        }
        task.setValue(Task.TITLE, title.trim());
    }

}
