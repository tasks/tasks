package com.todoroo.astrid.actfm.sync.messages;

import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;

@SuppressWarnings("nls")
public class ReplayOutstandingEntries<T extends RemoteModel, OE extends OutstandingEntry<T>> {

    private static final String ERROR_TAG = "actfm-replay-outstanding";

    protected final Class<T> modelClass;
    private final Class<OE> outstandingClass;
    private final String table;
    protected final RemoteModelDao<T> dao;
    protected final OutstandingEntryDao<OE> outstandingDao;
    private final boolean afterErrors;

    public ReplayOutstandingEntries(Class<T> modelClass, String table, RemoteModelDao<T> dao, OutstandingEntryDao<OE> outstandingDao, boolean afterErrors) {
        this.modelClass = modelClass;
        this.outstandingClass = DaoReflectionHelpers.getOutstandingClass(modelClass);
        this.table = table;
        this.dao = dao;
        this.outstandingDao = outstandingDao;
        this.afterErrors = afterErrors;
    }

    public void execute() {
        TodorooCursor<OE> outstanding = outstandingDao.query(Query.select(DaoReflectionHelpers.getModelProperties(outstandingClass))
                .orderBy(Order.asc(OutstandingEntry.ENTITY_ID_PROPERTY), Order.asc(OutstandingEntry.CREATED_AT_PROPERTY)));
        try {
            OE instance = outstandingClass.newInstance();
            for (outstanding.moveToFirst(); !outstanding.isAfterLast(); outstanding.moveToNext()) {
                instance.clear();
                instance.readPropertiesFromCursor(outstanding);
                processItem(instance.getValue(OutstandingEntry.ENTITY_ID_PROPERTY), instance, outstanding);
            }
        } catch (InstantiationException e) {
            Log.e(ERROR_TAG, "Error instantiating outstanding entry", e);
        } catch (IllegalAccessException e) {
            Log.e(ERROR_TAG, "Error instantiating outstanding entry", e);
        } catch (Exception e) {
            Log.e(ERROR_TAG, "Unexpected exception in replay outstanding entries", e);
        }
    }

    protected void enqueueChangesHappenedMessage(long id) {
        ActFmSyncThread.getInstance().enqueueMessage(new ChangesHappened<T, OE>(id, modelClass, dao, outstandingDao), null);
    }

    protected boolean shouldSaveModel(T model) {
        return true;
    }

    private void processItem(long id, OE instance, TodorooCursor<OE> outstanding) {
        try {
            T model = modelClass.newInstance();
            model.setId(id);
            OutstandingToModelVisitor<T> visitor = new OutstandingToModelVisitor<T>(model);
            int count = 0;
            for (; !outstanding.isAfterLast(); outstanding.moveToNext()) {
                instance.clear();
                instance.readPropertiesFromCursor(outstanding);
                if (instance.getValue(OutstandingEntry.ENTITY_ID_PROPERTY) != id)
                    break;
                count ++;
                String column = instance.getValue(OutstandingEntry.COLUMN_STRING_PROPERTY);
                Property<?> property = NameMaps.localColumnNameToProperty(table, column);
                // set values to model
                if (property != null)
                    property.accept(visitor, instance);
            }

            model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            if (shouldSaveModel(model)) {
                dao.saveExisting(model);

                if (count > 0 && !afterErrors) {
                    enqueueChangesHappenedMessage(id);
                }
            }

            outstanding.moveToPrevious(); // Move back one to undo the last iteration of the for loop
        } catch (InstantiationException e) {
            Log.e(ERROR_TAG, "Error instantiating model", e);
        } catch (IllegalAccessException e) {
            Log.e(ERROR_TAG, "Error instantiating model", e);
        }
    }

    private class OutstandingToModelVisitor<MTYPE extends T> implements PropertyVisitor<Void, OE> {

        private final MTYPE model;

        public OutstandingToModelVisitor(MTYPE model) {
            this.model = model;
        }

        @Override
        public Void visitInteger(Property<Integer> property, OE data) {
            Integer i = data.getMergedValues().getAsInteger(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (i != null)
                model.setValue(property, i);
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, OE data) {
            Long l = data.getMergedValues().getAsLong(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (l != null)
                model.setValue(property, l);
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, OE data) {
            Double d = data.getMergedValues().getAsDouble(OutstandingEntry.VALUE_STRING_PROPERTY.name);
            if (d != null)
                model.setValue(property, d);
            return null;
        }

        @Override
        public Void visitString(Property<String> property, OE data) {
            String s = data.getValue(OutstandingEntry.VALUE_STRING_PROPERTY);
            model.setValue(property, s);
            return null;
        }

    }

}
