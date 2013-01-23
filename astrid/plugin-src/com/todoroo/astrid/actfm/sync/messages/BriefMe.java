package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public class BriefMe<TYPE extends RemoteModel> extends ClientToServerMessage<TYPE> {

    public static <TYPE extends RemoteModel> BriefMe<TYPE> instantiateBriefMeForClass(Class<TYPE> cls, String pushedAtKey) {
        long pushedAt = Preferences.getLong(pushedAtKey, 0);
        return new BriefMe<TYPE>(cls, null, pushedAt);
    }

    public BriefMe(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        super(id, modelClass, modelDao);
    }

    public BriefMe(Class<TYPE> modelClass, String uuid, long pushedAt) {
        super(modelClass, uuid, pushedAt);
    }

    @Override
    protected boolean serializeExtrasToJSON(JSONObject serializeTo) throws JSONException {
        // No extras
        return true;
    }

    @Override
    protected String getTypeString() {
        return "BriefMe"; //$NON-NLS-1$
    }

}
