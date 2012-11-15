package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.DoubleProperty;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;

@SuppressWarnings("nls")
public class MakeChanges<TYPE extends RemoteModel> extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-make-changes";

    private final RemoteModelDao<TYPE> dao;
    private final String table;

    public MakeChanges(JSONObject json, RemoteModelDao<TYPE> dao) {
        super(json);
        table = json.optString("table");
        this.dao = dao;
    }

    @Override
    public void processMessage() {
        JSONObject changes = json.optJSONObject("changes");
        String uuid = json.optString("uuid");
        if (dao != null && changes != null && !TextUtils.isEmpty(uuid)) {
            try {
                TYPE model = dao.getModelClass().newInstance();
                JSONArray keys = changes.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String col = keys.optString(i);
                        if (!TextUtils.isEmpty(col)) {
                            Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, col);
                            if (property != null) { // Unsupported property
                                setPropertyFromJSON(model, property, changes, col);
                            }
                        }
                    }
                }

                if (model.getSetValues().size() > 0) {
                    if (dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), model) <= 0) { // If update doesn't update rows, create a new model
                        model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                        dao.createNew(model);
                    }
                }

            } catch (IllegalAccessException e) {
                Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
            } catch (InstantiationException e) {
                Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
            }
        }
    }

    private static void setPropertyFromJSON(AbstractModel model, Property<?> property, JSONObject json, String jsonKey) {
        if (property instanceof LongProperty) {
            try {
                long value = json.getLong(jsonKey);
                model.setValue((LongProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading long from JSON " + json + " with key " + jsonKey, e);
            }
        } else if (property instanceof StringProperty) {
            try {
                String value = json.getString(jsonKey);
                model.setValue((StringProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading string from JSON " + json + " with key " + jsonKey, e);
            }
        } else if (property instanceof IntegerProperty) {
            try {
                int value = json.getInt(jsonKey);
                model.setValue((IntegerProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading int from JSON " + json + " with key " + jsonKey, e);
            }
        } else if (property instanceof DoubleProperty) {
            try {
                double value = json.getDouble(jsonKey);
                model.setValue((DoubleProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading double from JSON " + json + " with key " + jsonKey, e);
            }
        }
    }

}
