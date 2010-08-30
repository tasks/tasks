/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.weloveastrid.rmilk.MilkUtilities;
import org.weloveastrid.rmilk.api.data.RtmList;
import org.weloveastrid.rmilk.api.data.RtmLists;
import org.weloveastrid.rmilk.sync.MilkTaskContainer;

import android.content.Context;
import android.database.CursorJoiner;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.StoreObjectApiDao;
import com.todoroo.astrid.data.StoreObjectApiDao.StoreObjectCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;

public final class MilkDataService {

    /** metadata key of tag addon */
    public static final String TAG_KEY = "tags-tag"; //$NON-NLS-1$

    // --- singleton

    private static MilkDataService instance = null;

    public static synchronized MilkDataService getInstance(Context context) {
        if(instance == null)
            instance = new MilkDataService(context);
        return instance;
    }

    // --- instance variables

    private final TaskApiDao taskDao;
    private final MetadataApiDao metadataDao;
    private final StoreObjectApiDao storeObjectDao;

    static final Random random = new Random();

    private MilkDataService(Context context) {
        // prevent instantiation
        taskDao = new TaskApiDao(context);
        storeObjectDao = new StoreObjectApiDao(context);
        metadataDao = new MetadataApiDao(context);
    }

    // --- task and metadata methods

    /**
     * Clears RTM metadata information. Used when user logs out of RTM
     */
    public void clearMetadata() {
        metadataDao.deleteWhere(Metadata.KEY.eq(MilkTaskFields.METADATA_KEY));
    }

    /**
     * Gets tasks that were modified since last sync. Used internally to
     * support the other methods.
     *
     * @param properties
     * @return cursor
     */
    private TodorooCursor<Task> getLocallyModified(Criterion criterion, Property<?>... properties) {
        long lastSyncDate = MilkUtilities.INSTANCE.getLastSyncDate();
        if(lastSyncDate == 0)
            return taskDao.query(Query.select(Task.ID).where(criterion).orderBy(Order.asc(Task.ID)));
        return
            taskDao.query(Query.select(properties).where(Criterion.and(criterion,
                    Task.MODIFICATION_DATE.gt(lastSyncDate))).orderBy(Order.asc(Task.ID)));
    }

    /**
     * Gets milk task metadata for joining
     *
     * @return cursor
     */
    private TodorooCursor<Metadata> getMilkTaskMetadata() {
        return metadataDao.query(Query.select(Metadata.TASK).where(
                MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY)).orderBy(Order.asc(Metadata.TASK)));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        TodorooCursor<Task> tasks = getLocallyModified(TaskCriteria.isActive(), Task.ID);
        TodorooCursor<Metadata> metadata = getMilkTaskMetadata();

        ArrayList<Long> matchingRows = new ArrayList<Long>();

        CursorJoiner joiner = new CursorJoiner(tasks, new String[] { Task.ID.name },
                metadata, new String[] { Metadata.TASK.name });
        for (CursorJoiner.Result joinerResult : joiner) {
            // only pick up tasks without metadata
            if(joinerResult == CursorJoiner.Result.LEFT) {
                matchingRows.add(tasks.getLong(0));
            }
        }

        return
            taskDao.query(Query.select(properties).where(Task.ID.in(matchingRows.toArray(new Long[matchingRows.size()]))));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        TodorooCursor<Task> tasks = getLocallyModified(TaskCriteria.isActive(), Task.ID);
        TodorooCursor<Metadata> metadata = getMilkTaskMetadata();

        ArrayList<Long> matchingRows = new ArrayList<Long>();

        CursorJoiner joiner = new CursorJoiner(tasks, new String[] { Task.ID.name },
                metadata, new String[] { Metadata.TASK.name });
        for (CursorJoiner.Result joinerResult : joiner) {
            // only pick up tasks with metadata
            if(joinerResult == CursorJoiner.Result.BOTH) {
                matchingRows.add(tasks.getLong(0));
            }
        }

        return
            taskDao.query(Query.select(properties).where(Task.ID.in(matchingRows.toArray(new Long[matchingRows.size()]))));
    }

    /**
     * Searches for a local task with same remote id, updates this task's id
     * @param remoteTask
     */
    public void findLocalMatch(MilkTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID)
            return;
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.TASK).
                where(Criterion.and(MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY),
                        MilkTaskFields.TASK_SERIES_ID.eq(remoteTask.taskSeriesId),
                        MilkTaskFields.TASK_ID.eq(remoteTask.taskId))));
        try {
            if(cursor.getCount() == 0)
                return;
            cursor.moveToFirst();
            remoteTask.task.setId(cursor.get(Metadata.TASK));
        } finally {
            cursor.close();
        }
    }

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(MilkTaskContainer task) {
        taskDao.save(task.task);

        task.metadata.add(MilkTaskFields.create(task));
        metadataDao.synchronizeMetadata(task.task.getId(), task.metadata,
                Criterion.or(MetadataCriteria.withKey(TAG_KEY),
                        MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY),
                        MetadataCriteria.withKey(MilkNoteFields.METADATA_KEY)));
    }

    /**
     * Reads a task and its metadata
     * @param task
     * @return
     */
    public MilkTaskContainer readTaskAndMetadata(TodorooCursor<Task> taskCursor) {
        Task task = new Task(taskCursor);

        // read tags, notes, etc
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        TodorooCursor<Metadata> metadataCursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.byTask(task.getId()),
                        Criterion.or(MetadataCriteria.withKey(TAG_KEY),
                                MetadataCriteria.withKey(MilkTaskFields.METADATA_KEY),
                                MetadataCriteria.withKey(MilkNoteFields.METADATA_KEY)))));
        try {
            for(metadataCursor.moveToFirst(); !metadataCursor.isAfterLast(); metadataCursor.moveToNext()) {
                metadata.add(new Metadata(metadataCursor));
            }
        } finally {
            metadataCursor.close();
        }

        return new MilkTaskContainer(task, metadata);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(
                MilkTaskFields.LIST_ID, MilkTaskFields.TASK_SERIES_ID, MilkTaskFields.TASK_ID, MilkTaskFields.REPEATING).where(
                MetadataCriteria.byTaskAndwithKey(taskId, MilkTaskFields.METADATA_KEY)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Reads task notes out of a task
     */
    public TodorooCursor<Metadata> getTaskNotesCursor(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, MilkNoteFields.METADATA_KEY)));
        return cursor;
    }

    // --- list methods

    private StoreObject[] lists = null;

    /**
     * Reads dashboards
     */
    private void readLists() {
        if(lists != null)
            return;

        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(MilkListFields.TYPE)).orderBy(Order.asc(MilkListFields.POSITION)));
        try {
            lists = new StoreObject[cursor.getCount()];
            for(int i = 0; i < lists.length; i++) {
                cursor.moveToNext();
                StoreObject list = new StoreObject(cursor);
                lists[i] = list;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * @return a list of lists
     */
    public StoreObject[] getLists() {
        readLists();
        return lists;
    }

    /**
     * Clears current cache of RTM lists and loads data from RTM into
     * database. Returns the inbox list.
     *
     * @param remoteLists
     * @return list with the name "inbox"
     */
    public StoreObject setLists(RtmLists remoteLists) {
        readLists();

        StoreObject inbox = null;
        for(Map.Entry<String, RtmList> remote : remoteLists.getLists().entrySet()) {
            if(remote.getValue().isSmart() || "All Tasks".equals(remote.getValue().getName())) //$NON-NLS-1$
                continue;

            long id = Long.parseLong(remote.getValue().getId());
            StoreObject local = null;
            for(StoreObject list : lists) {
                if(list.getValue(MilkListFields.REMOTE_ID).equals(id)) {
                    local = list;
                    break;
                }
            }

            if(local == null)
                local = new StoreObject();

            local.setValue(StoreObject.TYPE, MilkListFields.TYPE);
            local.setValue(MilkListFields.REMOTE_ID, id);
            local.setValue(MilkListFields.NAME, remote.getValue().getName());
            local.setValue(MilkListFields.POSITION, remote.getValue().getPosition());
            local.setValue(MilkListFields.ARCHIVED, remote.getValue().isArchived() ? 1 : 0);
            storeObjectDao.save(local);

            if(remote.getValue().isInbox()) {
                inbox = local;
            }
        }

        // clear dashboard cache
        lists = null;
        return inbox;
    }

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getListName(long listId) {
        readLists();
        for(StoreObject list : lists)
            if(list.getValue(MilkListFields.REMOTE_ID).equals(listId))
                return list.getValue(MilkListFields.NAME);
        return null;
    }

}
