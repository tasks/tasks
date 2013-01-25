package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.DoubleProperty;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.data.Task;


@SuppressWarnings("nls")
public class JSONChangeToPropertyVisitor implements PropertyVisitor<Void, String> {

    private static final String ERROR_TAG = "actfm-make-changes";

    private final AbstractModel model;
    private final JSONObject data;

    public JSONChangeToPropertyVisitor(AbstractModel model, JSONObject data) {
        this.model = model;
        this.data = data;
    }

    @Override
    public Void visitInteger(Property<Integer> property, String key) {
        try {
            int value;
            if (property.checkFlag(Property.PROP_FLAG_BOOLEAN)) {
                try {
                    value = data.getBoolean(key) ? 1 : 0;
                } catch (JSONException e) {
                    value = data.getInt(key);
                }
            } else {
                value = data.getInt(key);
            }
            model.setValue((IntegerProperty) property, value);
        } catch (JSONException e) {
            Log.e(ERROR_TAG, "Error reading int value with key " + key + " from JSON " + data, e);
        }
        return null;
    }

    @Override
    public Void visitLong(Property<Long> property, String key) {
        try {
            long value = data.optLong(key, 0);
            if (property.checkFlag(Property.PROP_FLAG_DATE)) {
                String valueString = data.getString(key);
                try {
                    value = DateUtilities.parseIso8601(valueString);
                    if (Task.DUE_DATE.equals(property)) {
                        value = Task.createDueDate(DateUtilities.isoStringHasTime(valueString) ? Task.URGENCY_SPECIFIC_DAY_TIME : Task.URGENCY_SPECIFIC_DAY, value);
                    }
                } catch (Exception e){
                    value = 0;
                }
            }
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
            if ("null".equals(value))
                value = "";
            else if (property.checkFlag(Property.PROP_FLAG_USER_ID) && ActFmPreferenceService.userId().equals(value))
                value = Task.USER_ID_SELF;
            if (property.equals(Task.USER_ID))
                model.setValue(Task.USER, ""); // Clear this value for migration purposes

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
