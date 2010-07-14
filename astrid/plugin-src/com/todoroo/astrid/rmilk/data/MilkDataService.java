/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.data;

import java.util.Map;
import java.util.Random;

import android.content.Context;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;

public class MilkDataService {

    // --- constants

    /** for joining milk task table with task table */
    public static final Join MILK_JOIN = Join.left(MilkTask.TABLE,
                    Task.ID.eq(MilkTask.TASK));

    // --- instance variables

    protected final Context context;

    private final MilkDatabase milkDatabase = new MilkDatabase();

    private final GenericDao<MilkList> milkListDao;
    private final GenericDao<MilkTask> milkTaskDao;

    @Autowired
    private TaskDao taskDao;

    static final Random random = new Random();

    public MilkDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
        milkListDao = new GenericDao<MilkList>(MilkList.class, milkDatabase);
        milkTaskDao = new GenericDao<MilkTask>(MilkTask.class, milkDatabase);
        milkDatabase.openForReading();
    }

    // --- task and metadata methods

    /**
     * Clears RTM metadata information. Used when user logs out of RTM
     */
    public void clearMetadata() {
        milkTaskDao.deleteWhere(Criterion.all);
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(MILK_JOIN).where(
                    Criterion.or(MilkTask.UPDATED.eq(0), MilkTask.TASK.isNull())));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(MILK_JOIN).
                    where(Criterion.and(MilkTask.UPDATED.neq(0),
                            MilkTask.UPDATED.lt(Task.MODIFICATION_DATE))));
    }

    // --- list methods

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getList(String listId) {
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

        // read all list counts
        TodorooCursor<MilkTask> cursor = milkTaskDao.query(Query.select(MilkList.ID, MilkList.NAME, COUNT).
            join(Join.inner(MilkList.TABLE, MilkTask.LIST_ID.eq(MilkList.ID))).
            orderBy(Order.asc(MilkList.POSITION), Order.asc(MilkList.ID)).
            groupBy(MilkTask.LIST_ID));
        ListContainer[] containers = new ListContainer[cursor.getCount()];
        try {
            for(int i = 0; i < containers.length; i++) {
                cursor.moveToNext();
                long id = cursor.get(MilkList.ID);
                String name = cursor.get(MilkList.NAME);
                int count = cursor.get(COUNT);
                containers[i] = new ListContainer(id, name);
                containers[i].count = count;
            }
            return containers;
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
     * Clears current cache of RTM lists and re-populates
     * @param lists
     */
    public void setLists(RtmLists lists) {
        milkListDao.deleteWhere(Criterion.all);
        MilkList model = new MilkList();
        for(Map.Entry<String, RtmList> list : lists.getLists().entrySet()) {
            model.setValue(MilkList.ID, Long.parseLong(list.getValue().getId()));
            model.setValue(MilkList.NAME, list.getValue().getName());
            model.setValue(MilkList.ARCHIVED, list.getValue().isArchived()? 1 : 0);
            milkListDao.createNew(model);
        }
    }

}
