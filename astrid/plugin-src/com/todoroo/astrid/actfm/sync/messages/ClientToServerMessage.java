package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public abstract class ClientToServerMessage<TYPE extends RemoteModel> {

    protected final Class<TYPE> modelClass;
    protected final long id;
    protected final String uuid;
    protected final long pushedAt;

    public ClientToServerMessage(Class<TYPE> modelClass, String uuid, long pushedAt) {
        this.modelClass = modelClass;
        this.uuid = uuid;
        this.pushedAt = pushedAt;
        this.id = AbstractModel.NO_ID;
    }

    public ClientToServerMessage(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        this.id = id;
        this.modelClass = modelClass;

        TYPE entity = getEntity(id, modelDao);
        if (entity == null) {
            this.uuid = RemoteModel.NO_UUID;
            this.pushedAt = 0;
        } else {
            this.uuid = entity.getValue(RemoteModel.UUID_PROPERTY);
            this.pushedAt = entity.getValue(RemoteModel.PUSHED_AT_PROPERTY);
        }
    }

    private TYPE getEntity(long localId, RemoteModelDao<TYPE> modelDao) {
        return modelDao.fetch(localId, RemoteModel.UUID_PROPERTY, RemoteModel.PUSHED_AT_PROPERTY);
    }

    public final String getUUID() {
        return uuid;
    }

    public final long getPushedAt() {
        return pushedAt;
    }

    public abstract void sendMessage();

}
