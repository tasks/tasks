package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public class RequestDoubleCheck<TYPE extends RemoteModel> extends ClientToServerMessage<TYPE> {

    public RequestDoubleCheck(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        super(id, modelClass, modelDao);
    }

    @Override
    public JSONObject serializeToJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE_KEY, "RequestDoubleCheck"); //$NON-NLS-1$
            json.put(TABLE_KEY, NameMaps.getServerNameForTable(table));
            json.put(UUID_KEY, uuid);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }
}
