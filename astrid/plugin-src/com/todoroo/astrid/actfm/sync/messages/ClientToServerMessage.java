package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

@SuppressWarnings("nls")
public abstract class ClientToServerMessage<TYPE extends RemoteModel> {

    protected final Class<TYPE> modelClass;
    protected final String table;
    protected final long id;
    protected final String uuid;
    protected final long pushedAt;

    public static final String TYPE_KEY = "type";
    public static final String TABLE_KEY = "table";
    public static final String UUID_KEY = "uuid";
    public static final String PUSHED_AT_KEY = "pushed_at";

    public ClientToServerMessage(Class<TYPE> modelClass, String uuid, long pushedAt) {
        this.modelClass = modelClass;
        Table tableClass = DaoReflectionHelpers.getStaticFieldByReflection(modelClass, Table.class, "TABLE");
        this.table = NameMaps.getServerNameForTable(tableClass);
        this.uuid = uuid;
        this.pushedAt = pushedAt;
        this.id = AbstractModel.NO_ID;
    }

    public ClientToServerMessage(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        this.id = id;
        this.modelClass = modelClass;
        Table tableClass = DaoReflectionHelpers.getStaticFieldByReflection(modelClass, Table.class, "TABLE");
        this.table = NameMaps.getServerNameForTable(tableClass);

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

    public final JSONObject serializeToJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE_KEY, getTypeString());
            json.put(TABLE_KEY, table);
            json.put(UUID_KEY, uuid);
            json.put(PUSHED_AT_KEY, pushedAt);
            serializeToJSONImpl(json);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    protected abstract void serializeToJSONImpl(JSONObject serializeTo) throws JSONException;
    protected abstract String getTypeString();

    public static ChangesHappened<?, ?> instantiateChangesHappened(Long id, ModelType modelType) {
        switch(modelType) {
        case TYPE_TASK:
            return new ChangesHappened<Task, TaskOutstanding>(id, Task.class,
                    PluginServices.getTaskDao(), PluginServices.getTaskOutstandingDao());
        case TYPE_TAG:
            return new ChangesHappened<TagData, TagOutstanding>(id, TagData.class,
                    PluginServices.getTagDataDao(), PluginServices.getTagOutstandingDao());
        default:
            return null;
        }
    }

}
