package com.todoroo.astrid.actfm.sync.messages;

import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public class BriefMe<TYPE extends RemoteModel> extends ClientToServerMessage<TYPE> {

    private static final String ERROR_TAG = "actfm-brief-me"; //$NON-NLS-1$

    public static final String TASK_ID_KEY = "task_id"; //$NON-NLS-1$
    public static final String TAG_ID_KEY = "tag_id"; //$NON-NLS-1$
    public static final String USER_ID_KEY = "user_id";  //$NON-NLS-1$

    public static <TYPE extends RemoteModel> BriefMe<TYPE> instantiateBriefMeForClass(Class<TYPE> cls, String pushedAtKey) {
        long pushedAt = Preferences.getLong(pushedAtKey, 0);
        return new BriefMe<TYPE>(cls, null, pushedAt);
    }

    public BriefMe(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao) {
        super(id, modelClass, modelDao);
        this.extraParameters = null;
    }

    private final Object[] extraParameters;

    public BriefMe(Class<TYPE> modelClass, String uuid, long pushedAt, Object...extraParameters) {
        super(modelClass, uuid, pushedAt);
        this.extraParameters = extraParameters;
    }

    @Override
    protected boolean serializeExtrasToJSON(JSONObject serializeTo, MultipartEntity entity) throws JSONException {
        if (extraParameters != null && extraParameters.length > 0) {
            for (int i = 0; i < extraParameters.length - 1; i+=2) {
                try {
                    String key = (String) extraParameters[i];
                    Object value = extraParameters[i + 1];
                    serializeTo.put(key, value);
                } catch (ClassCastException e) {
                    Log.e(ERROR_TAG, "ClassCastException serializing BriefMe", e); //$NON-NLS-1$
                } catch (JSONException e) {
                    Log.e(ERROR_TAG, "JSONException serializing BriefMe", e); //$NON-NLS-1$
                }
            }
        }
        return true;
    }

    @Override
    protected String getTypeString() {
        return "BriefMe"; //$NON-NLS-1$
    }

}
