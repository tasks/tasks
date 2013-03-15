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
        if (!item.containsValue(RemoteModel.UUID_PROPERTY) || RemoteModel.isUuidEmpty(item.getValue(RemoteModel.UUID_PROPERTY))) {
            item.setValue(RemoteModel.UUID_PROPERTY, UUIDHelper.newUUID());
        }
        return super.createNew(item);
    }

    private static int outstandingEntryFlag = -1;

    public static final int OUTSTANDING_FLAG_UNINITIALIZED = -1;
    public static final int OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING = 1 << 0;
    public static final int OUTSTANDING_ENTRY_FLAG_ENQUEUE_MESSAGES = 1 << 1;

    public static void setOutstandingEntryFlags(int newValue) {
        synchronized (RemoteModelDao.class) {
            outstandingEntryFlag = newValue;
        }
    }

    public static boolean getOutstandingEntryFlag(int flag) {
        if (outstandingEntryFlag == -1) {
            synchronized (RemoteModelDao.class) {
                int newValue = 0;
                if (PluginServices.getActFmPreferenceService().isLoggedIn())
                    newValue = OUTSTANDING_ENTRY_FLAG_ENQUEUE_MESSAGES | OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING;
                else if (PluginServices.getActFmPreferenceService().wasLoggedIn())
                    newValue = OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING;
                outstandingEntryFlag = newValue;
            }
        }

        return (outstandingEntryFlag & flag) > 0;
    }

    @Override
    protected boolean shouldRecordOutstanding(RTYPE item) {
        return super.shouldRecordOutstanding(item) && getOutstandingEntryFlag(OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
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

    public String uuidFromLocalId(long localId) {
        TodorooCursor<RTYPE> cursor = query(Query.select(RemoteModel.UUID_PROPERTY).where(AbstractModel.ID_PROPERTY.eq(localId)));
        try {
            if (cursor.getCount() == 0)
                return RemoteModel.NO_UUID;
            cursor.moveToFirst();
            return cursor.get(RemoteModel.UUID_PROPERTY);
        } finally {
            cursor.close();
        }
    }


}
