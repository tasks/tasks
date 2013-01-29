package com.todoroo.astrid.actfm.sync.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

@SuppressWarnings("nls")
public class ChangesHappened<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> extends ClientToServerMessage<TYPE> {

    private static final String ERROR_TAG = "actfm-changes-happened";

    private final Class<OE> outstandingClass;
    private final List<OE> changes;
    private final OutstandingEntryDao<OE> outstandingDao;

    public static final String CHANGES_KEY = "changes";

    public static ChangesHappened<?, ?> instantiateChangesHappened(Long id, ModelType modelType) {
        switch(modelType) {
        case TYPE_TASK:
            return new ChangesHappened<Task, TaskOutstanding>(id, Task.class,
                    PluginServices.getTaskDao(), PluginServices.getTaskOutstandingDao());
        case TYPE_TAG:
            return new ChangesHappened<TagData, TagOutstanding>(id, TagData.class,
                    PluginServices.getTagDataDao(), PluginServices.getTagOutstandingDao());
        default:
            return null;
        }
    }

    public ChangesHappened(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao,
            OutstandingEntryDao<OE> outstandingDao) {
        super(id, modelClass, modelDao);

        this.outstandingClass = DaoReflectionHelpers.getOutstandingClass(modelClass);
        this.outstandingDao = outstandingDao;
        this.changes = new ArrayList<OE>();

    }

    @Override
    protected boolean serializeExtrasToJSON(JSONObject serializeTo) throws JSONException {
        // Process changes list and serialize to JSON
        JSONArray changesJson = changesToJSON();
        if (changesJson == null || changesJson.length() == 0)
            return false;
        serializeTo.put(CHANGES_KEY, changesJson);
        return true;
    }

    @Override
    protected String getTypeString() {
        return "ChangesHappened";
    }

    public List<OE> getChanges() {
        return changes;
    }

    private JSONArray changesToJSON() {
        if (!RemoteModel.NO_UUID.equals(uuid))
            populateChanges();

        JSONArray array = new JSONArray();
        PropertyToJSONVisitor visitor = new PropertyToJSONVisitor();
        for (OE change : changes) {
            try {
                String localColumn = change.getValue(OutstandingEntry.COLUMN_STRING_PROPERTY);
                JSONObject changeJson = new JSONObject();
                changeJson.put("id", change.getId());
                String serverColumn;
                if (NameMaps.TAG_ADDED_COLUMN.equals(localColumn)) {
                    serverColumn = NameMaps.TAG_ADDED_COLUMN;
                    changeJson.put("value", change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));
                } else if (NameMaps.TAG_REMOVED_COLUMN.equals(localColumn)) {
                    serverColumn = NameMaps.TAG_REMOVED_COLUMN;
                    changeJson.put("value", change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));
                } else {
                    Property<?> localProperty = NameMaps.localColumnNameToProperty(table, localColumn);
                    if (localProperty == null)
                        throw new RuntimeException("No local property found for local column " + localColumn + " in table " + table);

                    serverColumn = NameMaps.localColumnNameToServerColumnName(table, localColumn);
                    if (serverColumn == null)
                        throw new RuntimeException("No server column found for local column " + localColumn + " in table " + table);

                    changeJson.put("value", localProperty.accept(visitor, change));
                }

                changeJson.put("column", serverColumn);

                array.put(changeJson);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error writing change to JSON", e);
            }
        }
        return array;
    }

    private void populateChanges() {
        TodorooCursor<OE> cursor = outstandingDao.query(Query.select(DaoReflectionHelpers.getModelProperties(outstandingClass))
               .where(OutstandingEntry.ENTITY_ID_PROPERTY.eq(id)).orderBy(Order.asc(OutstandingEntry.CREATED_AT_PROPERTY)));
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                try {
                    OE instance = outstandingClass.newInstance();
                    instance.readPropertiesFromCursor(cursor);
                    changes.add(instance);
                } catch (IllegalAccessException e) {
                    Log.e("ChangesHappened", "Error instantiating outstanding model class", e);
                } catch (InstantiationException e2) {
                    Log.e("ChangesHappened", "Error instantiating outstanding model class", e2);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private class PropertyToJSONVisitor implements PropertyVisitor<Object, OE> {

        private String getAsString(OE data) {
            return data.getValue(OutstandingEntry.VALUE_STRING_PROPERTY);
        }

        @Override
        public Object visitInteger(Property<Integer> property, OE data) {
            Integer i = data.getMergedValues().getAsInteger(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (i != null) {
                if (property.checkFlag(Property.PROP_FLAG_BOOLEAN))
                    return i > 0;
                return i;
            } else {
                return getAsString(data);
            }
        }

        @Override
        public Object visitLong(Property<Long> property, OE data) {
            Long l = data.getMergedValues().getAsLong(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (l != null) {
                if (property.checkFlag(Property.PROP_FLAG_DATE)) {
                    boolean includeTime = true;
                    if (Task.DUE_DATE.equals(property) && !Task.hasDueTime(l))
                        includeTime = false;
                    return DateUtilities.timeToIso8601(l, includeTime);
                }
                return l;
            } else {
                return getAsString(data);
            }
        }

        @Override
        public Object visitDouble(Property<Double> property, OE data) {
            Double d = data.getMergedValues().getAsDouble(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (d != null) {
                return d;
            } else {
                return getAsString(data);
            }
        }

        @Override
        public Object visitString(Property<String> property, OE data) {
            String value = getAsString(data);
            if (RemoteModel.NO_UUID.equals(value) && property.checkFlag(Property.PROP_FLAG_USER_ID))
                return ActFmPreferenceService.userId();
            return value;
        }

    }

}
