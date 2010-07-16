/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.util.Log;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.Utilities;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;
import com.todoroo.astrid.rmilk.sync.RTMTaskContainer;
import com.todoroo.astrid.tags.TagService;

public final class MilkDataService {

    // --- constants

    /** Utility for joining tasks with metadata */
    public static final Join METADATA_JOIN = Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK));

    // --- singleton

    private static MilkDataService instance = null;

    public static synchronized MilkDataService getInstance() {
        if(instance == null)
            instance = new MilkDataService(ContextManager.getContext());
        return instance;
    }

    // --- instance variables

    protected final Context context;

    private final MilkDatabase milkDatabase = new MilkDatabase();

    private final GenericDao<MilkList> milkListDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

    static final Random random = new Random();

    private MilkDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
        milkListDao = new GenericDao<MilkList>(MilkList.class, milkDatabase);
        milkDatabase.openForReading();
    }

    // --- task and metadata methods

    /**
     * Clears RTM metadata information. Used when user logs out of RTM
     */
    public void clearMetadata() {
        metadataDao.deleteWhere(Metadata.KEY.eq(MilkTask.METADATA_KEY));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(MilkDataService.METADATA_JOIN).where(Criterion.and(
                    Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                            where(Criterion.and(MetadataCriteria.withKey(MilkTask.METADATA_KEY), MilkTask.TASK_SERIES_ID.gt(0))))),
                    TaskCriteria.isActive())));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        long lastSyncDate = Utilities.getLastSyncDate();
        if(lastSyncDate == 0)
            return taskDao.query(Query.select(Task.ID).where(Criterion.none));
        return
            taskDao.query(Query.select(properties).join(MilkDataService.METADATA_JOIN).
                    where(Criterion.and(MetadataCriteria.withKey(MilkTask.METADATA_KEY),
                            Task.MODIFICATION_DATE.gt(lastSyncDate))));
    }

    /**
     * Searches for a local task with same remote id, updates this task's id
     * @param remoteTask
     */
    public void findLocalMatch(RTMTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID)
            return;
        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID).
                join(MilkDataService.METADATA_JOIN).where(Criterion.and(MetadataCriteria.withKey(MilkTask.METADATA_KEY),
                        MilkTask.TASK_SERIES_ID.eq(remoteTask.taskSeriesId),
                        MilkTask.TASK_ID.eq(remoteTask.taskId))));
        try {
            if(cursor.getCount() == 0)
                return;
            cursor.moveToFirst();
            remoteTask.task.setId(cursor.get(Task.ID));
        } finally {
            cursor.close();
        }
    }

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(RTMTaskContainer task) {
        Log.e("SAV", "saving " + task.task.getSetValues());
        taskDao.save(task.task, true);

        metadataDao.deleteWhere(Criterion.and(MetadataCriteria.byTask(task.task.getId()),
                Criterion.or(MetadataCriteria.withKey(MilkTask.METADATA_KEY),
                        MetadataCriteria.withKey(MilkNote.METADATA_KEY),
                        MetadataCriteria.withKey(TagService.KEY))));
        task.metadata.add(MilkTask.create(task));
        for(Metadata metadata : task.metadata) {
            metadata.setValue(Metadata.TASK, task.task.getId());
            metadataDao.persist(metadata);
        }
    }

    /**
     * Reads a task and its metadata
     * @param task
     * @return
     */
    public RTMTaskContainer readTaskAndMetadata(TodorooCursor<Task> taskCursor) {
        Task task = new Task(taskCursor);

        // read tags, notes, etc
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        TodorooCursor<Metadata> metadataCursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.byTask(task.getId()),
                        Criterion.or(MetadataCriteria.withKey(TagService.KEY),
                                MetadataCriteria.withKey(MilkTask.METADATA_KEY),
                                MetadataCriteria.withKey(MilkNote.METADATA_KEY)))));
        try {
            for(metadataCursor.moveToFirst(); !metadataCursor.isAfterLast(); metadataCursor.moveToNext()) {
                metadata.add(new Metadata(metadataCursor));
            }
        } finally {
            metadataCursor.close();
        }

        return new RTMTaskContainer(task, metadata);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(
                MilkTask.LIST_ID, MilkTask.TASK_SERIES_ID, MilkTask.TASK_ID, MilkTask.REPEATING).where(
                MetadataCriteria.byTaskAndwithKey(taskId, MilkTask.METADATA_KEY)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    // --- list methods

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getListName(long listId) {
        TodorooCursor<MilkList> cursor = milkListDao.query(Query.select(
                MilkList.NAME).where(MilkList.ID.eq(listId)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return cursor.get(MilkList.NAME);
        } finally {
            cursor.close();
        }
    }

    /**
     * Get RTM lists as container objects
     * @return
     */
    public ListContainer[] getListsWithCounts() {
        CountProperty COUNT = new CountProperty();

        // read list names
        TodorooCursor<MilkList> listCursor = milkListDao.query(Query.select(MilkList.ID,
                MilkList.NAME).where(MilkList.ARCHIVED.eq(0)).orderBy(Order.asc(MilkList.POSITION)));
        ListContainer[] lists = new ListContainer[listCursor.getCount()];
        HashMap<Long, ListContainer> listIdToContainerMap;
        try {
            int length = listCursor.getCount();
            if(length == 0)
                return lists;
            listIdToContainerMap = new HashMap<Long, ListContainer>(length);
            MilkList list = new MilkList();
            for(int i = 0; i < length; i++) {
                listCursor.moveToNext();
                list.readFromCursor(listCursor);
                lists[i] = new ListContainer(list);
                listIdToContainerMap.put(list.getId(), lists[i]);
            }
        } finally {
            listCursor.close();
        }

        // read all list counts
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(MilkTask.LIST_ID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(Criterion.and(TaskCriteria.isVisible(DateUtilities.now()), TaskCriteria.isActive())).
            groupBy(MilkTask.LIST_ID));
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                ListContainer container = listIdToContainerMap.get(cursor.get(MilkTask.LIST_ID));
                if(container != null) {
                    container.count = cursor.get(COUNT);
                }
            }
            return lists;
        } finally {
            cursor.close();
        }
    }

    /**
     * Get RTM lists as strings
     * @return
     */
    /*public ListContainer[] getLists() {
        // read all list titles
        milkDatabase.open(context);
        TodorooCursor<MilkList> cursor = milkListDao.fetch(milkDatabase,
                List.PROPERTIES, null, List.ID + " ASC"); //$NON-NLS-1$
        ListContainer[] containers = new ListContainer[cursor.getCount()];
        try {
            List list = new List();
            for(int i = 0; i < containers.length; i++) {
                cursor.moveToNext();
                list.readFromCursor(cursor, List.PROPERTIES);
                ListContainer container = new ListContainer(list);
                containers[i] = container;
            }
            return containers;
        } finally {
            cursor.close();
            milkDatabase.close();
        }
    }*/

    /**
     * Clears current cache of RTM lists and re-populates. Returns the inbox
     * list.
     *
     * @param lists
     * @return list with the name "inbox"
     */
    public MilkList setLists(RtmLists lists) {
        milkListDao.deleteWhere(Criterion.all);
        MilkList model = new MilkList();
        MilkList inbox = null;
        for(Map.Entry<String, RtmList> list : lists.getLists().entrySet()) {
            if(list.getValue().isSmart() || "All Tasks".equals(list.getValue().getName())) //$NON-NLS-1$
                continue;
            model.setValue(MilkList.ID, Long.parseLong(list.getValue().getId()));
            model.setValue(MilkList.NAME, list.getValue().getName());
            model.setValue(MilkList.POSITION, list.getValue().getPosition());
            model.setValue(MilkList.ARCHIVED, list.getValue().isArchived()? 1 : 0);
            milkListDao.createNew(model);

            if(list.getValue().isInbox()) {
                inbox = model;
                model = new MilkList();
            }
        }
        return inbox;
    }

}
