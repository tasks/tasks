package com.todoroo.astrid.actfm.sync.messages;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class ChangesHappened<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> extends ClientToServerMessage<TYPE> {

    private static final String ERROR_TAG = "actfm-changes-happened";

    private final Class<OE> outstandingClass;
    private final List<OE> changes;

    public static final String CHANGES_KEY = "changes";

    public ChangesHappened(long id, Class<TYPE> modelClass, RemoteModelDao<TYPE> modelDao,
            OutstandingEntryDao<OE> outstandingDao) {
        super(id, modelClass, modelDao);

        this.outstandingClass = getOutstandingClass(modelClass);
        this.changes = new ArrayList<OE>();

        if (!RemoteModel.NO_UUID.equals(uuid))
            populateChanges(outstandingDao);
    }

    @Override
    public JSONObject serializeToJSON() {
        // Process changes list and serialize to JSON
        JSONObject json = new JSONObject();
        try {
            String serverTable = NameMaps.getServerNameForTable(table);
            json.put(TYPE_KEY, "ChangesHappened");
            json.put(TABLE_KEY, serverTable);
            json.put(UUID_KEY, uuid);
            json.put(PUSHED_AT_KEY, pushedAt);
            json.put(CHANGES_KEY, changesToJSON(serverTable));
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    public List<OE> getChanges() {
        return changes;
    }

    public int numChanges() {
        return changes.size();
    }

    private JSONArray changesToJSON(String tableString) {
        JSONArray array = new JSONArray();
        for (OE change : changes) {
            try {
                String localColumn = change.getValue(OutstandingEntry.COLUMN_STRING_PROPERTY);
                String serverColumn = NameMaps.localColumnNameToServerColumnName(tableString, localColumn);
                if (serverColumn == null)
                    throw new RuntimeException("No server column found for local column " + localColumn + " in table " + tableString);

                JSONObject changeJson = new JSONObject();
                changeJson.put("id", change.getId());
                changeJson.put("column", serverColumn);
                changeJson.put("value", change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));

                array.put(changeJson);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error writing change to JSON", e);
            }
        }
        return array;
    }

    private void populateChanges(OutstandingEntryDao<OE> outstandingDao) {
        TodorooCursor<OE> cursor = outstandingDao.query(Query.select(getModelProperties(outstandingClass))
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

    private Class<OE> getOutstandingClass(Class<? extends RemoteModel> model) {
        return DaoReflectionHelpers.getStaticFieldByReflection(model, Class.class, "OUTSTANDING_MODEL");
    }

    private Property<?>[] getModelProperties(Class<? extends AbstractModel> model) {
        return DaoReflectionHelpers.getStaticFieldByReflection(model, Property[].class, "PROPERTIES");
    }
}
