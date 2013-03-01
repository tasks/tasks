package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.helper.UUIDHelper;

/**
 * This class is meant to be subclassed for daos whose models
 * require UUID generation (i.e., most RemoteModels). The createNew
 * method takes care of automatically generating a new UUID for each newly
 * created model if one doesn't already exist.
 * @author Sam
 *
 * @param <RTYPE>
 */
public class RemoteModelDao<RTYPE extends RemoteModel> extends DatabaseDao<RTYPE> {

    public RemoteModelDao(Class<RTYPE> modelClass) {
        super(modelClass);
    }

    @Override
    public boolean createNew(RTYPE item) {
        if (!item.containsValue(RemoteModel.UUID_PROPERTY)) {
            item.setValue(RemoteModel.UUID_PROPERTY, UUIDHelper.newUUID());
        }
        return super.createNew(item);
    }

    public static int outstandingEntryFlag = -1;

    public static boolean getOutstandingEntryFlag() {
        if (outstandingEntryFlag == -1) {
            synchronized (RemoteModelDao.class) {
                if (PluginServices.getActFmPreferenceService().isLoggedIn())
                    outstandingEntryFlag = 1;
                else
                    outstandingEntryFlag = 0;
            }
        }
        return outstandingEntryFlag > 0;
    }

    @Override
    protected boolean shouldRecordOutstanding(RTYPE item) {
        return super.shouldRecordOutstanding(item) && getOutstandingEntryFlag();
    }

    /**
     * Fetch a model object by UUID
     * @param uuid
     * @param properties
     * @return
     */
    public RTYPE fetch(String uuid, Property<?>... properties) {
        TodorooCursor<RTYPE> cursor = fetchItem(uuid, properties);
        return returnFetchResult(cursor);
    }

    /**
     * Returns cursor to object corresponding to the given identifier
     *
     * @param database
     * @param table
     *            name of table
     * @param properties
     *            properties to read
     * @param id
     *            id of item
     * @return
     */
    protected TodorooCursor<RTYPE> fetchItem(String uuid, Property<?>... properties) {
        TodorooCursor<RTYPE> cursor = query(
                Query.select(properties).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
        cursor.moveToFirst();
        return new TodorooCursor<RTYPE>(cursor, properties);
    }

    /**
     * Get the local id
     * @param uuid
     * @return
     */
    public long localIdFromUuid(String uuid) {
        TodorooCursor<RTYPE> cursor = query(Query.select(AbstractModel.ID_PROPERTY).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
        try {
            if (cursor.getCount() == 0)
                return AbstractModel.NO_ID;
            cursor.moveToFirst();
            return cursor.get(AbstractModel.ID_PROPERTY);
        } finally {
            cursor.close();
        }
    }


}
