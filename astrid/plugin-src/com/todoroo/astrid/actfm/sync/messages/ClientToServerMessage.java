package com.todoroo.astrid.actfm.sync.messages;

import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.crittercism.app.Crittercism;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public abstract class ClientToServerMessage<TYPE extends RemoteModel> {

    protected final Class<TYPE> modelClass;
    protected final String table;
    protected final long id;
    protected final String uuid;
    protected final long pushedAt;
    protected final boolean foundEntity;

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
        this.foundEntity = true;
        this.id = AbstractModel.NO_ID;
    }

    public ClientToServerMessage(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        this.id = id;
        this.modelClass = modelClass;
        Table tableClass = DaoReflectionHelpers.getStaticFieldByReflection(modelClass, Table.class, "TABLE");
        this.table = NameMaps.getServerNameForTable(tableClass);

        TYPE entity = getEntity(id, modelDao);
        this.foundEntity = entity != null;
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

    public final JSONObject serializeToJSON(MultipartEntity entity) {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE_KEY, getTypeString());
            json.put(TABLE_KEY, table);
            json.put(UUID_KEY, uuid);
            String dateValue = DateUtilities.timeToIso8601(pushedAt, true);
            json.put(PUSHED_AT_KEY, dateValue != null ? dateValue : 0);
            if (serializeExtrasToJSON(json, entity))
                return json;
            else
                return null;
        } catch (JSONException e) {
            Crittercism.logHandledException(e);
            return null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((table == null) ? 0 : table.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClientToServerMessage<?> other = (ClientToServerMessage<?>) obj;
        if (table == null) {
            if (other.table != null)
                return false;
        } else if (!table.equals(other.table))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    protected abstract boolean serializeExtrasToJSON(JSONObject serializeTo, MultipartEntity entity) throws JSONException;
    protected abstract String getTypeString();
}
