package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.weloveastrid.rmilk.data.MilkTaskFields;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.tags.TagService;
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
    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private ExceptionService exceptionService;

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
                newTask.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
                newTask.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
                taskDao.save(newTask);
                long newId = newTask.getId();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    metadata.readFromCursor(cursor);

                    if(!metadata.containsNonNullValue(Metadata.KEY))
                        continue;

                    if(GtasksMetadata.METADATA_KEY.equals(metadata.getValue(Metadata.KEY)))
                        metadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
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
    public boolean quickAdd(Task task) {
        ArrayList<String> tags = new ArrayList<String>();
        boolean quickAddMarkup = false;
        try {
            quickAddMarkup = parseQuickAddMarkup(task, tags);
        } catch (Throwable e) {
            exceptionService.reportError("parse-quick-add", e); //$NON-NLS-1$
        }
        save(task);

        Metadata metadata = new Metadata();
        for(String tag : tags) {
            metadata.setValue(Metadata.KEY, TagService.KEY);
            metadata.setValue(Metadata.TASK, task.getId());
            metadata.setValue(TagService.TAG, tag);
            metadataDao.createNew(metadata);
        }
        return quickAddMarkup;
    }

    /**
     * Parse quick add markup for the given task
     * @param task
     * @param tags an empty array to apply tags to
     * @return
     */
    public static boolean parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        return new TitleParser(task, tags).parse();
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
        long userId = clone.getValue(Task.USER_ID);
        if (userId != Task.USER_ID_SELF && userId != ActFmPreferenceService.userId())
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
    public static Task createWithValues(ContentValues values, String title,
            TaskService taskService, MetadataService metadataService) {
        Task task = new Task();
        return createWithValues(task, values, title, taskService, metadataService);
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
    public static Task createWithValues(Task task, ContentValues values, String title,
            TaskService taskService, MetadataService metadataService) {
        if (title != null)
            task.setValue(Task.TITLE, title);

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
            task.mergeWith(forTask);
        }

        boolean markup = taskService.quickAdd(task);
        if (markup)
            task.putTransitory(TRANS_QUICK_ADD_MARKUP, true);

        if (forMetadata != null && forMetadata.size() > 0) {
            Metadata metadata = new Metadata();
            metadata.setValue(Metadata.TASK, task.getId());
            metadata.mergeWith(forMetadata);
            metadataService.save(metadata);
        }

        return task;
    }

}
