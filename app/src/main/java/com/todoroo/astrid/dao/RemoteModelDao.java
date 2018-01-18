package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;

/**
 * This class is meant to be subclassed for daos whose models
 * require UUID generation (i.e., most RemoteModels). The createNew
 * method takes care of automatically generating a new UUID for each newly
 * created model if one doesn't already exist.
 * @author Sam
 *
 */
public class RemoteModelDao extends DatabaseDao {

    public RemoteModelDao(Database database) {
        super(database);
    }

    @Override
    public boolean createNew(Task item) {
        if (!item.containsValue(RemoteModel.UUID_PROPERTY) || RemoteModel.isUuidEmpty(item.getUuidProperty())) {
            item.setUuidProperty(UUIDHelper.newUUID());
        }
        return super.createNew(item);
    }
}
