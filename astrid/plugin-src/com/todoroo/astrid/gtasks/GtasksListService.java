package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksListService {

    public static final String LIST_NOT_FOUND = null;
    public static final StoreObject LIST_NOT_FOUND_OBJECT = null;

    @Autowired
    private StoreObjectDao storeObjectDao;

    private StoreObject[] lists = null;

    public GtasksListService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private void readLists() {
        if(lists != null)
            return;

        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(GtasksList.TYPE)));
        try {
            lists = new StoreObject[cursor.getCount()];
            for(int i = 0; i < lists.length; i++) {
                cursor.moveToNext();
                StoreObject dashboard = new StoreObject(cursor);
                lists[i] = dashboard;
            }
        } finally {
            cursor.close();
        }
    }

    public StoreObject[] getLists() {
        readLists();
        return lists;
    }

    /**
     * Get list name
     * @param listId
     * @return NOT_FOUND if no list by this id exists, otherwise list name
     */
    public String getListName(String listId) {
        StoreObject list = getList(listId);
        if(list != LIST_NOT_FOUND_OBJECT)
            return list.getValue(GtasksList.NAME);
        return LIST_NOT_FOUND;
    }

    public void updateLists(GoogleTaskListInfo[] remoteLists) {
        readLists();
        for(int i = 0; i < remoteLists.length; i++) {
            GoogleTaskListInfo remote = remoteLists[i];

            String id = remote.getId();
            StoreObject local = null;
            for(StoreObject list : lists) {
                if(list.getValue(GtasksList.REMOTE_ID).equals(id)) {
                    local = list;
                    break;
                }
            }

            if(local == null)
                local = new StoreObject();

            local.setValue(StoreObject.TYPE, GtasksList.TYPE);
            local.setValue(GtasksList.REMOTE_ID, id);
            local.setValue(GtasksList.NAME, remote.getName());
            local.setValue(GtasksList.ORDER, i);
            storeObjectDao.persist(local);
        }

        clearListCache();
    }

    private void clearListCache() {
        lists = null;
    }

    public StoreObject getList(String listId) {
        readLists();
        for(StoreObject list : lists)
            if(list.getValue(GtasksList.REMOTE_ID).equals(listId))
                return list;
        return LIST_NOT_FOUND_OBJECT;
    }

}
