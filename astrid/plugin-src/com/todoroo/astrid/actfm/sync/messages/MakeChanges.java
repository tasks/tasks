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
import com.todoroo.andlib.data.Property.PropertyVisitor;
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
                    JSONToPropertyVisitor visitor = new JSONToPropertyVisitor(changes, model);
                    for (int i = 0; i < keys.length(); i++) {
                        String col = keys.optString(i);
                        if (!TextUtils.isEmpty(col)) {
                            Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, col);
                            if (property != null) { // Unsupported property
                                property.accept(visitor, col);
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


    private static class JSONToPropertyVisitor implements PropertyVisitor<Void, String> {

        private final JSONObject json;
        private final AbstractModel model;

        public JSONToPropertyVisitor(JSONObject json, AbstractModel model) {
            this.json = json;
            this.model = model;
        }

        @Override
        public Void visitInteger(Property<Integer> property, String data) {
            try {
                int value = json.getInt(data);
                model.setValue((IntegerProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading int from JSON " + json + " with key " + data, e);
            }
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, String data) {
            try {
                long value = json.getLong(data);
                model.setValue((LongProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading long from JSON " + json + " with key " + data, e);
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, String data) {
            try {
                double value = json.getDouble(data);
                model.setValue((DoubleProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading double from JSON " + json + " with key " + data, e);
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, String data) {
            try {
                String value = json.getString(data);
                model.setValue((StringProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading string from JSON " + json + " with key " + data, e);
            }
            return null;
        }

    }
}
