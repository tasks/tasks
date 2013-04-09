package com.todoroo.astrid.actfm.sync.messages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.crittercism.app.Crittercism;
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
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskAttachmentOutstanding;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.UserActivityOutstanding;
import com.todoroo.astrid.data.WaitingOnMe;
import com.todoroo.astrid.data.WaitingOnMeOutstanding;

@SuppressWarnings("nls")
public class ChangesHappened<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> extends ClientToServerMessage<TYPE> {

    private static final String ERROR_TAG = "actfm-changes-happened";

    protected final Class<OE> outstandingClass;
    protected final List<OE> changes;
    protected final OutstandingEntryDao<OE> outstandingDao;

    public static final String CHANGES_KEY = "changes";

    public static ChangesHappened<?, ?> instantiateChangesHappened(long id, ModelType modelType) {
        switch(modelType) {
        case TYPE_TASK:
            return new ChangesHappened<Task, TaskOutstanding>(id, Task.class,
                    PluginServices.getTaskDao(), PluginServices.getTaskOutstandingDao());
        case TYPE_TAG:
            return new ChangesHappened<TagData, TagOutstanding>(id, TagData.class,
                    PluginServices.getTagDataDao(), PluginServices.getTagOutstandingDao());
        case TYPE_ACTIVITY:
            return new ChangesHappened<UserActivity, UserActivityOutstanding>(id, UserActivity.class,
                    PluginServices.getUserActivityDao(), PluginServices.getUserActivityOutstandingDao());
        case TYPE_ATTACHMENT:
            return new ChangesHappened<TaskAttachment, TaskAttachmentOutstanding>(id, TaskAttachment.class,
                    PluginServices.getTaskAttachmentDao(), PluginServices.getTaskAttachmentOutstandingDao());
        case TYPE_TASK_LIST_METADATA:
            return new TaskListMetadataChangesHappened(id, TaskListMetadata.class,
                    PluginServices.getTaskListMetadataDao(), PluginServices.getTaskListMetadataOutstandingDao());
        case TYPE_WAITING_ON_ME:
            return new ChangesHappened<WaitingOnMe, WaitingOnMeOutstanding>(id, WaitingOnMe.class,
                    PluginServices.getWaitingOnMeDao(), PluginServices.getWaitingOnMeOutstandingDao());
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

        if (!foundEntity) // Stop sending changes for entities that don't exist anymore
            outstandingDao.deleteWhere(OutstandingEntry.ENTITY_ID_PROPERTY.eq(id));
    }

    @Override
    protected boolean serializeExtrasToJSON(JSONObject serializeTo, MultipartEntity entity) throws JSONException {
        // Process changes list and serialize to JSON
        JSONArray changesJson = changesToJSON(entity);
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

    private JSONArray changesToJSON(MultipartEntity entity) {
        if (!RemoteModel.NO_UUID.equals(uuid))
            populateChanges();

        JSONArray array = new JSONArray();
        AtomicInteger uploadCounter = new AtomicInteger();
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
                } else if (NameMaps.MEMBER_ADDED_COLUMN.equals(localColumn)) {
                    serverColumn = NameMaps.MEMBER_ADDED_COLUMN;
                    changeJson.put("value", change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));
                } else if (NameMaps.MEMBER_REMOVED_COLUMN.equals(localColumn)) {
                    serverColumn = NameMaps.MEMBER_REMOVED_COLUMN;
                    changeJson.put("value", change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));
                } else if (NameMaps.ATTACHMENT_ADDED_COLUMN.equals(localColumn)) {
                    serverColumn = NameMaps.ATTACHMENT_ADDED_COLUMN;
                    JSONObject fileJson = getFileJson(change.getValue(OutstandingEntry.VALUE_STRING_PROPERTY));
                    String name = fileJson == null ? null : addToEntityFromFileJson(entity, fileJson, uploadCounter);
                    if (name == null) {
                        PluginServices.getTaskAttachmentDao().delete(id);
                        PluginServices.getTaskAttachmentOutstandingDao().deleteWhere(TaskAttachmentOutstanding.ENTITY_ID_PROPERTY.eq(id));
                        return null;
                    }
                    changeJson.put("value", name);
                } else {
                    Property<?> localProperty = NameMaps.localColumnNameToProperty(table, localColumn);
                    if (localProperty == null)
                        throw new RuntimeException("No local property found for local column " + localColumn + " in table " + table);

                    serverColumn = NameMaps.localColumnNameToServerColumnName(table, localColumn);
                    if (serverColumn == null)
                        throw new RuntimeException("No server column found for local column " + localColumn + " in table " + table);

                    Object value = localProperty.accept(visitor, change);
                    if (!validateValue(localProperty, value))
                        return null;

                    if (value == null)
                        changeJson.put("value", JSONObject.NULL);
                    else {
                        if (localProperty.checkFlag(Property.PROP_FLAG_PICTURE) && value instanceof JSONObject) {
                            JSONObject json = (JSONObject) value;
                            String name = addToEntityFromFileJson(entity, json, uploadCounter);
                            if (name != null)
                                changeJson.put("value", name);
                        } else {
                            changeJson.put("value", value);
                        }
                    }
                }

                changeJson.put("column", serverColumn);

                String createdAt = DateUtilities.timeToIso8601(change.getValue(OutstandingEntry.CREATED_AT_PROPERTY), true);
                changeJson.put("created_at", createdAt != null ? createdAt : 0);

                array.put(changeJson);
            } catch (JSONException e) {
                Log.e(ERROR_TAG, "Error writing change to JSON", e);
                Crittercism.logHandledException(e);
            }
        }
        return array;
    }

    private String addToEntityFromFileJson(MultipartEntity entity, JSONObject json, AtomicInteger uploadCounter) {
        if (json.has("path")) {
            String path = json.optString("path");
            String name = String.format("upload-%s-%s-%d", table, uuid, uploadCounter.get());
            String type = json.optString("type");
            File f = new File(path);
            if (f.exists()) {
                json.remove("path");
                entity.addPart(name, new FileBody(f, type));
                return name;
            }
        }
        return null;
    }

    protected void populateChanges() {
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

    // Return false if value is detected to be something that would definitely cause a server error
    // (e.g. empty task title, etc.)
    private boolean validateValue(Property<?> property, Object value) {
        if (Task.TITLE.equals(property)) {
            if (!(value instanceof String) || TextUtils.isEmpty((String) value))
                return false;
        }
        return true;
    }

    private JSONObject getFileJson(String value) {
        try {
            JSONObject obj = new JSONObject(value);
            String path = obj.optString("path");
            if (TextUtils.isEmpty(path))
                return null;
            File f = new File(path);
            if (!f.exists())
                return null;

            return obj;
        } catch (JSONException e) {
            return null;
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
            if (property.checkFlag(Property.PROP_FLAG_JSON)) {
                if (TextUtils.isEmpty(value))
                    return null;
                try {
                    if (value != null && value.startsWith("["))
                        return new JSONArray(value);
                    else
                        return new JSONObject(value);
                } catch (JSONException e) {
                    return null;
                }
            }
            return value;
        }

    }

}
