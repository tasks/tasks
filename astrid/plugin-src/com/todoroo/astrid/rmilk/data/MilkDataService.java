/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.Utilities;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;

public final class MilkDataService {

    // --- public constants

    /** metadata key */
    public static final String METADATA_KEY = "rmilk"; //$NON-NLS-1$

    /** {@link MilkList} id */
    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** RTM Task Series Id */
    public static final LongProperty TASK_SERIES_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** RTM Task Id */
    public static final LongProperty TASK_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** Whether task repeats in RTM (1 or 0) */
    public static final IntegerProperty REPEATING = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

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
        metadataDao.deleteWhere(Metadata.KEY.eq(METADATA_KEY));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(METADATA_JOIN).where(Criterion.and(
                    Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                            where(Criterion.and(MetadataCriteria.withKey(METADATA_KEY), TASK_SERIES_ID.gt(0))))),
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
            taskDao.query(Query.select(properties).join(METADATA_JOIN).
                    where(Criterion.and(MetadataCriteria.withKey(METADATA_KEY),
                            Task.MODIFICATION_DATE.gt(lastSyncDate))));
    }

    /**
     * Searches for a local task with same remote id, updates this task's id
     * @param task
     */
    public void updateFromLocalCopy(Task task) {
        if(task.getId() != Task.NO_ID)
            return;
        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID).
                join(METADATA_JOIN).where(Criterion.and(MetadataCriteria.withKey(METADATA_KEY),
                        TASK_SERIES_ID.eq(task.getValue(TASK_SERIES_ID)),
                        TASK_ID.eq(task.getValue(TASK_ID)))));
        try {
            if(cursor.getCount() == 0)
                return;
            cursor.moveToFirst();
            task.setId(cursor.get(Task.ID));
        } finally {
            cursor.close();
        }
    }

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(Task task) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(LIST_ID, task.getValue(LIST_ID));
        metadata.setValue(TASK_SERIES_ID, task.getValue(TASK_SERIES_ID));
        metadata.setValue(TASK_ID, task.getValue(TASK_ID));
        metadata.setValue(REPEATING, task.getValue(REPEATING));

        task.clearValue(LIST_ID);
        task.clearValue(TASK_SERIES_ID);
        task.clearValue(TASK_ID);
        task.clearValue(REPEATING);

        taskDao.save(task, true);
        metadata.setValue(Metadata.TASK, task.getId());

        metadataDao.deleteWhere(MetadataCriteria.byTaskAndwithKey(task.getId(),
                METADATA_KEY));
        metadataDao.persist(metadata);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(
                LIST_ID, TASK_SERIES_ID, TASK_ID, REPEATING).where(
                MetadataCriteria.byTaskAndwithKey(taskId, METADATA_KEY)));
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
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(LIST_ID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(Criterion.and(TaskCriteria.isVisible(DateUtilities.now()), TaskCriteria.isActive())).
            groupBy(LIST_ID));
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                ListContainer container = listIdToContainerMap.get(cursor.get(LIST_ID));
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
