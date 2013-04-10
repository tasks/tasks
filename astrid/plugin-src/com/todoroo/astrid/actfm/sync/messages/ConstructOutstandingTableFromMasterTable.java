package com.todoroo.astrid.actfm.sync.messages;

import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class ConstructOutstandingTableFromMasterTable<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> {

    protected final String table;
    protected final RemoteModelDao<TYPE> dao;
    protected final OutstandingEntryDao<OE> outstandingDao;
    protected final LongProperty createdAtProperty;

    public ConstructOutstandingTableFromMasterTable(String table, RemoteModelDao<TYPE> dao,
            OutstandingEntryDao<OE> outstandingDao, LongProperty createdAtProperty) {
        this.table = table;
        this.dao = dao;
        this.outstandingDao = outstandingDao;
        this.createdAtProperty = createdAtProperty;
    }

    protected void extras(long itemId, long createdAt) {
        // Subclasses can override
    }

    public void execute() {
        execute(Criterion.all);
    }

    public void execute(Criterion criterion) {
        Property<?>[] syncableProperties = NameMaps.syncableProperties(table);
        TodorooCursor<TYPE> items = dao.query(Query.select(AndroidUtilities.addToArray(Property.class, syncableProperties, AbstractModel.ID_PROPERTY, RemoteModel.UUID_PROPERTY)).where(criterion));
        try {
            OE oe = outstandingDao.getModelClass().newInstance();
            for (items.moveToFirst(); !items.isAfterLast(); items.moveToNext()) {
                long createdAt;
                if (createdAtProperty != null)
                    createdAt = items.get(createdAtProperty);
                else
                    createdAt = DateUtilities.now();
                long itemId = items.get(AbstractModel.ID_PROPERTY);
                for (Property<?> p : syncableProperties) {
                    oe.clear();
                    oe.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, itemId);
                    oe.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, p.name);
                    Object value = items.get(p);
                    if (value == null)
                        continue;

                    oe.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, value.toString());
                    oe.setValue(OutstandingEntry.CREATED_AT_PROPERTY, createdAt);
                    outstandingDao.createNew(oe);
                }
                extras(itemId, createdAt);
            }
        } catch (IllegalAccessException e) {
            Log.e("ConstructOutstanding", "Error instantiating outstanding model class", e);
        } catch (InstantiationException e2) {
            Log.e("ConstructOutstanding", "Error instantiating outstanding model class", e2);
        } finally {
            items.close();
        }
    }

}
