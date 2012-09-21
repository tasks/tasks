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
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class ChangesHappened<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> implements ClientToServerMessage {

    private final Class<? extends RemoteModel> modelClass;
    private final Class<OE> outstandingClass;
    private final long id;
    private final long uuid;
    private final List<OE> changes;
    private long pushedAt;
    private final OutstandingEntryDao<OE> outstandingDao;

    public ChangesHappened(TYPE entity, RemoteModelDao<TYPE> modelDao,
            OutstandingEntryDao<OE> outstandingDao) {
        this.modelClass = entity.getClass();
        this.outstandingClass = getOutstandingClass(modelClass);
        this.id = entity.getId();
        this.changes = new ArrayList<OE>();
        this.outstandingDao = outstandingDao;

        if (!entity.containsValue(RemoteModel.REMOTE_ID_PROPERTY)
                || !entity.containsValue(RemoteModel.PUSHED_AT_PROPERTY)) {
            entity = modelDao.fetch(entity.getId(), getModelProperties(modelClass));
        }
        if (entity == null) {
            this.uuid = 0;
            this.pushedAt = 0;
        } else {
            this.uuid = entity.getValue(RemoteModel.REMOTE_ID_PROPERTY);
            this.pushedAt = entity.getValue(RemoteModel.PUSHED_AT_PROPERTY);
            populateChanges();
        }
    }

    public void sendMessage() {
        // Process changes list and send to server
    }

    public List<OE> getChanges() {
        return changes;
    }

    private void populateChanges() {
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
        try {
            return (Class<OE>) getStaticFieldByReflection(model, "OUTSTANDING_MODEL");
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
