package com.todoroo.astrid.actfm.sync.messages;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class ChangesHappened<TYPE extends RemoteModel> implements ClientToServerMessage {

    private final Class<? extends RemoteModel> modelClass;
    private final Class<? extends OutstandingEntry<TYPE>> outstandingClass;
    private final Property<?>[] outstandingProperties;
    private final long id;
    private final long uuid;
    private final List<Change> changes;
    private long pushedAt; // TODO: Populate and use
    private final OutstandingEntryDao<OutstandingEntry<TYPE>> dao;

    public ChangesHappened(TYPE entity, OutstandingEntryDao<OutstandingEntry<TYPE>> dao) {
        this.modelClass = entity.getClass();
        this.outstandingClass = getOutstandingClass(modelClass);
        this.outstandingProperties = getModelProperties(outstandingClass);
        this.id = entity.getId();
        this.uuid = entity.getValue(RemoteModel.REMOTE_ID_PROPERTY);
        this.changes = new ArrayList<Change>();
        this.dao = dao;
        populateChanges();
    }

    public void sendMessage() {
        // Process changes list and send to server
    }

    private class Change {
        public final long uuid;
        public final OutstandingEntry<TYPE> outstandingEntry;

        public Change(long uuid, OutstandingEntry<TYPE> outstandingEntry) {
            this.uuid = uuid;
            this.outstandingEntry = outstandingEntry;
        }
    }

    private void populateChanges() {
        TodorooCursor<OutstandingEntry<TYPE>> cursor = dao.query(Query.select(outstandingProperties)
               .where(OutstandingEntry.ENTITY_ID_PROPERTY.eq(id)).orderBy(Order.asc(OutstandingEntry.CREATED_AT_PROPERTY)));
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                try {
                    OutstandingEntry<TYPE> instance = outstandingClass.newInstance();
                    instance.readPropertiesFromCursor(cursor);
                    changes.add(new Change(uuid, instance));
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

    private Class<? extends OutstandingEntry<TYPE>> getOutstandingClass(Class<? extends RemoteModel> model) {
        try {
            return (Class<? extends OutstandingEntry<TYPE>>) getStaticFieldByReflection(model, "OUTSTANDING_MODEL");
        } catch (ClassCastException e) {
            throw new RuntimeException("OUTSTANDING_MODEL field for class " + model.getName() + " is not of the correct type");
        }
    }

    private Property<?>[] getModelProperties(Class<? extends AbstractModel> model) {
        try {
            return (Property<?>[]) getStaticFieldByReflection(model, "PROPERTIES");
        } catch (ClassCastException e) {
            throw new RuntimeException("PROPERTIES field for class " + model.getName() + " is not of the correct type");
        }
    }

    private Object getStaticFieldByReflection(Class<?> cls, String fieldName) {
        try {
            Field field = cls.getField(fieldName);
            Object obj = field.get(null);
            if (obj == null) {
                throw new RuntimeException(fieldName + " field for class " + cls.getName() + " is null");
            }
            return obj;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class " + cls.getName() + " does not declare field " + fieldName);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException(fieldName + " field for class " + cls.getName() + " is not accessible");
        }
    }

}
