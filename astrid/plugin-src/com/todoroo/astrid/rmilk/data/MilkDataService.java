/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.database.sqlite.SQLiteQueryBuilder;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.Utilities;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;
import com.todoroo.astrid.service.MetadataService;

@SuppressWarnings("nls")
public class MilkDataService {

    protected final Context context;

    private final MilkDatabase milkDatabase = new MilkDatabase();

    @Autowired
    private MetadataService metadataService;

    private final GenericDao<MilkList> listDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private Database database;

    static final Random random = new Random();

    public MilkDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
        listDao = new GenericDao<MilkList>(MilkList.class, milkDatabase);
    }

    // --- RTM properties

    /** RTM List id */
    public static final StringJoinProperty LIST_ID =
        new StringJoinProperty(Utilities.KEY_LIST_ID);

    /** RTM Task Series id */
    public static final StringJoinProperty TASK_SERIES_ID =
        new StringJoinProperty(Utilities.KEY_TASK_SERIES_ID);

    /** RTM Task id */
    public static final StringJoinProperty TASK_ID =
        new StringJoinProperty(Utilities.KEY_TASK_ID);

    /** 1 if task repeats in RTM, 0 otherwise */
    public static final IntegerJoinProperty REPEAT =
        new IntegerJoinProperty(Utilities.KEY_REPEAT);

    /** 1 if task was updated since last sync, 0 otherwise */
    public static final IntegerJoinProperty UPDATED =
        new IntegerJoinProperty(Utilities.KEY_UPDATED);

    // --- non-RTM properties we synchronize

    public static final StringJoinProperty TAGS =
        new StringJoinProperty(com.todoroo.astrid.tags.TagService.KEY);

    // --- task and metadata methods

    /** Properties to fetch when user wants to view / edit tasks */
    public static final Property<?>[] RTM_PROPERTIES = new Property<?>[] {
        Task.ID,
        LIST_ID,
        TASK_SERIES_ID,
        TASK_ID,
        REPEAT
    };

    /**
     * Read a single task by task id
     * @param taskId
     * @return item, or null if it doesn't exist
     */
    public Task readTask(long taskId) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(createJoinClause(RTM_PROPERTIES));
        TodorooCursor<Task> cursor =
            taskDao.query(database, RTM_PROPERTIES, builder,
                    Task.ID.qualifiedName()+ " = " + taskId,
                    null, null);
        try {
            if (cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            Task task = new Task(cursor, RTM_PROPERTIES);
            return task;
        } finally {
            cursor.close();
        }
    }

    /** Helper method for building a join clause for task/metadata joins */
    public static String createJoinClause(Property<?>[] properties) {
        StringBuilder stringBuilder = new StringBuilder(Database.TASK_TABLE);
        int joinTableCount = 0;
        for(Property<?> property : properties) {
            if(property instanceof JoinProperty) {
                JoinProperty jp = (JoinProperty)property;
                stringBuilder.append(" LEFT JOIN (").append(jp.joinTable()).
                    append(") m").append(++joinTableCount).
                    append(" ON ").append(Task.ID_PROPERTY).append(" = m").
                    append(joinTableCount).append('.').append(Metadata.TASK.name).
                    append(' ');
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Clears RTM metadata information. Used when user logs out of RTM
     */
    public void clearMetadata() {
        metadataService.deleteWhere(String.format("%s = '%s' OR %s = '%s' " +
        		"OR %s = '%s OR %s = '%s'",
        		Metadata.KEY, LIST_ID,
        		Metadata.KEY, TASK_SERIES_ID,
        		Metadata.KEY, TASK_ID,
        		Metadata.KEY, REPEAT));
    }

    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(createJoinClause(properties));
        TodorooCursor<Task> cursor =
            taskDao.query(database, properties, builder,
                    TASK_ID + " ISNULL",
                    null, null);
        return cursor;
    }

    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(createJoinClause(properties));
        TodorooCursor<Task> cursor =
            taskDao.query(database, properties, builder,
                    "NOT " + TASK_ID + " ISNULL", // TODO wrong!
                    null, null);
        return cursor;
    }

    // --- list methods

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getList(String listId) {
        pluginDatabase.open(context);
        TodorooCursor<List> cursor = listDao.fetch(pluginDatabase,
                List.PROPERTIES, ListSql.withId(listId), null, "1"); //$NON-NLS-1$
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return cursor.get(List.NAME);
        } finally {
            cursor.close();
            pluginDatabase.close();
        }
    }

    /**
     * Get RTM lists as container objects
     * @return
     */
    public ListContainer[] getListsWithCounts() {
        // read all list titles
        pluginDatabase.open(context);
        TodorooCursor<List> listCursor = listDao.fetch(pluginDatabase,
                List.PROPERTIES, null);
        HashMap<String, ListContainer> listIdToContainerMap;
        try {
            int count = listCursor.getCount();
            if(count == 0)
                return new ListContainer[0];

            listIdToContainerMap =
                new HashMap<String, ListContainer>(count);
            List list = new List();
            for(int i = 0; i < count; i++) {
                listCursor.moveToNext();
                list.readFromCursor(listCursor, List.PROPERTIES);
                ListContainer container = new ListContainer(list);
                listIdToContainerMap.put(container.id, container);
            }
        } finally {
            listCursor.close();
        }

        // read all list counts
        IntegerProperty countProperty = Property.countProperty();
        TodorooCursor<Metadata> metadataCursor = metadataService.fetchWithCount(
                MetadataSql.withKey(Utilities.KEY_LIST_ID), Metadata.VALUE + " ASC", false); //$NON-NLS-1$
        ListContainer[] containers = new ListContainer[metadataCursor.getCount()];
        try {
            for(int i = 0; i < containers.length; i++) {
                metadataCursor.moveToNext();
                String id = metadataCursor.get(Metadata.VALUE);
                ListContainer container = listIdToContainerMap.get(id);
                if(container == null) {
                    container = new ListContainer(id, "[unknown]"); //$NON-NLS-1$
                }
                container.count = metadataCursor.get(countProperty);
                containers[i] = container;
            }
            return containers;
        } finally {
            metadataCursor.close();
            pluginDatabase.close();
        }
    }

    /**
     * Get RTM lists as strings
     * @return
     */
    public ListContainer[] getLists() {
        // read all list titles
        pluginDatabase.open(context);
        TodorooCursor<List> cursor = listDao.fetch(pluginDatabase,
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
            pluginDatabase.close();
        }
    }

    /**
     * Clears current cache of RTM lists and re-populates
     * @param lists
     */
    public void setLists(RtmLists lists) {
        pluginDatabase.open(context);
        try {
            List model = new List();
            for(Map.Entry<String, RtmList> list : lists.getLists().entrySet()) {
                model.setValue(List.ID, list.getValue().getId());
                model.setValue(List.NAME, list.getValue().getName());
                listDao.save(pluginDatabase, model);
            }
        } finally {
            pluginDatabase.close();
        }
    }

}
