package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public class BriefMe<TYPE extends RemoteModel> extends ClientToServerMessage<TYPE> {

    public BriefMe(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        super(id, modelClass, modelDao);
    }

    public BriefMe(Class<TYPE> modelClass, String uuid, long pushedAt) {
        super(modelClass, uuid, pushedAt);
    }

    @Override
    public JSONObject serializeToJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE_KEY, "BriefMe"); //$NON-NLS-1$
            json.put(TABLE_KEY, NameMaps.getServerNameForTable(table));
            json.put(UUID_KEY, uuid);
            json.put(PUSHED_AT_KEY, pushedAt);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

}
