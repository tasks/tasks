package com.todoroo.astrid.actfm.sync.messages;

import java.text.ParseException;

import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class NowBriefed<TYPE extends RemoteModel> extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-now-briefed";

    private final RemoteModelDao<TYPE> dao;
    private final String table;
    private final String uuid;
    private long pushedAt;

    public NowBriefed(JSONObject json, RemoteModelDao<TYPE> dao) {
        super(json);
        this.table = json.optString("table");
        this.uuid = json.optString("uuid");
        this.dao = dao;
        try {
            this.pushedAt = DateUtilities.parseIso8601(json.optString("pushed_at"));
        } catch (ParseException e) {
            this.pushedAt = 0;
        }
    }

    @Override
    public void processMessage() {
        if (pushedAt > 0) {
            if (TextUtils.isEmpty(uuid)) {
                String pushedAtKey = null;
                if (NameMaps.TABLE_ID_TASKS.equals(table))
                    pushedAtKey = NameMaps.PUSHED_AT_TASKS;
                else if (NameMaps.TABLE_ID_TAGS.equals(table))
                    pushedAtKey = NameMaps.PUSHED_AT_TAGS;
                else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
                    pushedAtKey = NameMaps.PUSHED_AT_ACTIVITY;

                if (pushedAtKey != null)
                    Preferences.setLong(pushedAtKey, pushedAt);

            } else {
                try {
                    TYPE instance = dao.getModelClass().newInstance();
                    instance.setValue(RemoteModel.PUSHED_AT_PROPERTY, pushedAt);
                    dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), instance);
                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for NowBriefed", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for NowBriefed", e);
                }
            }
        }
    }

}
