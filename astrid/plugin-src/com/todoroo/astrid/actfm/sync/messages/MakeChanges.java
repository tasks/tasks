package com.todoroo.astrid.actfm.sync.messages;

import java.util.Iterator;

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
        JSONObject changes = json.optJSONObject("changes");
        String uuid = json.optString("uuid");
        if (changes != null && !TextUtils.isEmpty(uuid)) {
            if (dao != null) {
                try {
                    TYPE model = dao.getModelClass().newInstance();
                    JSONChangeToPropertyVisitor visitor = new JSONChangeToPropertyVisitor(model, changes);
                    Iterator<String> keys = changes.keys();
                    while (keys.hasNext()) {
                        String column = keys.next();
                        Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, column);
                        if (property != null) { // Unsupported property
                            property.accept(visitor, column);
                        }
                    }

                    if (model.getSetValues().size() > 0) {
                        model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                        if (dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), model) <= 0) { // If update doesn't update rows. create a new model
                            model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                            dao.createNew(model);
                        }
                    }

                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                }
            } else if (NameMaps.TABLE_ID_PUSHED_AT.equals(table)) {
                long pushedAt = changes.optLong(NameMaps.TABLE_ID_PUSHED_AT);
                if (pushedAt > 0) {
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


    private static class JSONChangeToPropertyVisitor implements PropertyVisitor<Void, String> {

        private final AbstractModel model;
        private final JSONObject data;

        public JSONChangeToPropertyVisitor(AbstractModel model, JSONObject data) {
            this.model = model;
            this.data = data;
        }

        @Override
        public Void visitInteger(Property<Integer> property, String key) {
            try {
                int value = data.getInt(key);
                model.setValue((IntegerProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading int value with key " + key + " from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, String key) {
            try {
                long value = data.getLong(key);
                model.setValue((LongProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading long value with key " + key + " from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, String key) {
            try {
                double value = data.getDouble(key);
                model.setValue((DoubleProperty) property, value);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error reading double value with key " + key + " from JSON " + data, e);
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, String key) {
            try {
                String value = data.getString(key);
                model.setValue((StringProperty) property, value);
            } catch (JSONException e) {
                try {
                    JSONObject object = data.getJSONObject(key);
                    if (object != null)
                        model.setValue((StringProperty) property, object.toString());
                } catch (JSONException e2) {
                    Log.e(ERROR_TAG, "Error reading JSON value with key " + key + " from JSON " + data, e);
                }
                Log.e(ERROR_TAG, "Error reading string value with key " + key + " from JSON " + data, e);
            }
            return null;
        }
    }
}
