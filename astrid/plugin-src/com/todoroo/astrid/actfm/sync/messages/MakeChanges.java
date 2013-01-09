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
import com.todoroo.andlib.utility.Preferences;
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
        JSONArray changes = json.optJSONArray("changes");
        String uuid = json.optString("uuid");
        if (changes != null && !TextUtils.isEmpty(uuid)) {
            if (dao != null) { // Model case
                try {
                    TYPE model = dao.getModelClass().newInstance();
                    if (changes.length() > 0) {
                        JSONChangeToPropertyVisitor visitor = new JSONChangeToPropertyVisitor(model);
                        for (int i = 0; i < changes.length(); i++) {
                            JSONArray change = changes.optJSONArray(i);

                            if (change != null) {
                                String column = change.optString(0);
                                Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, column);
                                if (property != null) { // Unsupported property
                                    property.accept(visitor, change);
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
            } else if (NameMaps.TABLE_ID_PUSHED_AT.equals(table)) { // pushed_at case
                JSONArray change = changes.optJSONArray(0);
                if (change != null && change.optString(0).equals(NameMaps.TABLE_ID_PUSHED_AT)) {
                    long pushedAt = change.optLong(1);
                    String pushedAtKey = null;
                    if (NameMaps.TABLE_ID_TASKS.equals(uuid))
                        pushedAtKey = NameMaps.PUSHED_AT_TASKS;
                    else if (NameMaps.TABLE_ID_TAGS.equals(uuid))
                        pushedAtKey = NameMaps.PUSHED_AT_TAGS;

                    if (pushedAtKey != null)
                        Preferences.setLong(pushedAtKey, pushedAt);

                }
            }
        }
    }


    private static class JSONChangeToPropertyVisitor implements PropertyVisitor<Void, JSONArray> {

        private final AbstractModel model;

        public JSONChangeToPropertyVisitor(AbstractModel model) {
            this.model = model;
        }

        @Override
        public Void visitInteger(Property<Integer> property, JSONArray data) {
            try {
                int value = data.getInt(1);
                model.setValue((IntegerProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading int value from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, JSONArray data) {
            try {
                long value = data.getLong(1);
                model.setValue((LongProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading long value from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, JSONArray data) {
            try {
                double value = data.getDouble(1);
                model.setValue((DoubleProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading double value from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, JSONArray data) {
            try {
                String value = data.getString(1);
                model.setValue((StringProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading string value from JSON " + data, e);
            }
            return null;
        }

    }
}
