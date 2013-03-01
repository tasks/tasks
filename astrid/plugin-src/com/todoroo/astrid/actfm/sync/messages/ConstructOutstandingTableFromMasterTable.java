package com.todoroo.astrid.actfm.sync.messages;

import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

@SuppressWarnings("nls")
public class ConstructOutstandingTableFromMasterTable<TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> {

    private final String table;
    private final RemoteModelDao<TYPE> dao;
    private final OutstandingEntryDao<OE> outstandingDao;
    private final LongProperty createdAtProperty;

    public ConstructOutstandingTableFromMasterTable(String table, RemoteModelDao<TYPE> dao,
            OutstandingEntryDao<OE> outstandingDao, LongProperty createdAtProperty) {
        this.table = table;
        this.dao = dao;
        this.outstandingDao = outstandingDao;
        this.createdAtProperty = createdAtProperty;
    }

    public void execute() {
        Property<?>[] syncableProperties = NameMaps.syncableProperties(table);
        TodorooCursor<TYPE> items = dao.query(Query.select(AndroidUtilities.addToArray(syncableProperties, AbstractModel.ID_PROPERTY, RemoteModel.UUID_PROPERTY)));
        try {
            OE oe = outstandingDao.getModelClass().newInstance();
            for (items.moveToFirst(); !items.isAfterLast(); items.moveToNext()) {
                long itemId = items.get(AbstractModel.ID_PROPERTY);
                for (Property<?> p : syncableProperties) {
                    oe.clear();
                    oe.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, itemId);
                    oe.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, p.name);
                    oe.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, items.get(p).toString());
                    if (createdAtProperty != null)
                        oe.setValue(OutstandingEntry.CREATED_AT_PROPERTY, items.get(createdAtProperty));
                    else
                        oe.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());
                    outstandingDao.createNew(oe);
                }
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
