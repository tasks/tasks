package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
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

    /**
     * Fetch a model object by UUID
     */
    public RTYPE fetch(String uuid, Property<?>... properties) {
        TodorooCursor<RTYPE> cursor = fetchItem(uuid, properties);
        return returnFetchResult(cursor);
    }

    /**
     * Returns cursor to object corresponding to the given identifier
     *
     * @param properties
     *            properties to read
     */
    protected TodorooCursor<RTYPE> fetchItem(String uuid, Property<?>... properties) {
        TodorooCursor<RTYPE> cursor = query(
                Query.select(properties).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
        cursor.moveToFirst();
        return new TodorooCursor<>(cursor, properties);
    }

    public String uuidFromLocalId(long localId) {
        TodorooCursor<RTYPE> cursor = query(Query.select(RemoteModel.UUID_PROPERTY).where(AbstractModel.ID_PROPERTY.eq(localId)));
        try {
            if (cursor.getCount() == 0) {
                return RemoteModel.NO_UUID;
            }
            cursor.moveToFirst();
            return cursor.get(RemoteModel.UUID_PROPERTY);
        } finally {
            cursor.close();
        }
    }


}
