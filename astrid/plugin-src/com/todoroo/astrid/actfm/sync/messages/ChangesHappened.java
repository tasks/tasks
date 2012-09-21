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

    public void send() {
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
            Field outstandingField = model.getField("OUTSTANDING_MODEL");
            Class<? extends OutstandingEntry<TYPE>> outstanding = (Class<? extends OutstandingEntry<TYPE>>) outstandingField.get(null);
            if (outstanding == null) {
                throw new RuntimeException("OUTSTANDING_MODEL field for class " + model.getName() + " is null");
            }
            return outstanding;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class " + model.getName() + " does not declare an OUTSTANDING_MODEL field");
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("OUTSTANDING_MODEL field for class " + model.getName() + " is not accessible");
        } catch (ClassCastException e3) {
            throw new RuntimeException("OUTSTANDING_MODEL field for class " + model.getName() + " is not of the correct type");
        }
    }

    private Property<?>[] getModelProperties(Class<? extends AbstractModel> model) {
        try {
            Field propertiesField = model.getField("PROPERTIES");
            Property<?>[] properties = (Property<?>[]) propertiesField.get(null);
            if (properties == null) {
                throw new RuntimeException("PROPERTIES field for class " + model.getName() + " is null");
            }
            return properties;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class " + model.getName() + " does not declare an PROPERTIES field");
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("PROPERTIES field for class " + model.getName() + " is not accessible");
        } catch (ClassCastException e3) {
            throw new RuntimeException("PROPERTIES field for class " + model.getName() + " is not of the correct type");
        }
    }

}
