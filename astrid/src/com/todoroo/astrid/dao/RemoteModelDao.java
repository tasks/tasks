package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.utility.Pair;
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
        if (!item.containsValue(RemoteModel.REMOTE_ID_PROPERTY)) {
            Pair<Long, String> uuidPair = UUIDHelper.newUUID();
            item.setValue(RemoteModel.REMOTE_ID_PROPERTY, uuidPair.getLeft());
            item.setValue(RemoteModel.PROOF_TEXT_PROPERTY, uuidPair.getRight());
        }
        return super.createNew(item);
    };


}
